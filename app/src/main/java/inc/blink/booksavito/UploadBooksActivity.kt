package inc.blink.booksavito


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import inc.blink.booksavito.databinding.ActivityUploadBooksBinding
import inc.blink.booksavito.initializeFirebase
import inc.blink.booksavito.models.Book
import java.text.SimpleDateFormat
import java.util.*

class UploadBooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBooksBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""

    // Контракт для выбора файла
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeFirebase(this)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
        updateUIState(UploadState.IDLE)
        val storage = Firebase.storage
        val reference = storage.reference

        // Проверяем корневой доступ
        reference.listAll()
            .addOnSuccessListener { listResult ->
                // Bucket существует и доступен
                Log.d("Storage", "Bucket доступен")
            }
            .addOnFailureListener { exception ->
                Log.e("Storage", "Ошибка доступа к bucket: ${exception.message}")
            }
    }

    private fun setupClickListeners() {
        // Кнопка выбора файла
        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        // Кнопка загрузки
        binding.btnUpload.setOnClickListener {
            uploadBook()
        }

        // Кнопка повтора
        binding.btnRetry.setOnClickListener {
            uploadBook()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/plain",
                "application/epub+zip",
                "application/pdf"
            ))
        }
        filePickerLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        selectedFileUri = uri

        // Получаем имя файла
        val fileName = getFileNameFromUri(uri)
        selectedFileName = fileName

        // Обновляем UI
        binding.tvSelectedFile.text = "Выбран файл: $fileName"

        // Автозаполнение названия книги из имени файла
        val title = fileName.substringBeforeLast(".")
        binding.etTitle.setText(title)
    }

    private fun getFileNameFromUri(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex("_display_name")
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "unknown_file"
        } catch (e: Exception) {
            "unknown_file"
        }
    }

    private fun uploadBook() {
        if (!validateInput()) {
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        updateUIState(UploadState.UPLOADING)

        val fileUri = selectedFileUri!!
        val fileName = selectedFileName
        val fileType = getFileType(fileName)
        val storagePath = "books/${currentUser.uid}/${System.currentTimeMillis()}_$fileName"

        // Ссылка на Storage
        val storageRef = storage.reference.child(storagePath)

        // Загрузка файла в Storage
        val uploadTask = storageRef.putFile(fileUri)

        // Слушатель прогресса
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            binding.progressBar.progress = progress
            binding.tvProgress.text = "$progress%"
        }

        // Обработка завершения загрузки
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Получаем URL загруженного файла
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                // Сохраняем метаданные в Firestore
                saveBookToFirestore(
                    fileName = fileName,
                    fileType = fileType,
                    fileUrl = downloadUrl.toString(),
                    fileSize = taskSnapshot.totalByteCount,
                    userId = currentUser.uid
                )
            }.addOnFailureListener { exception ->
                updateUIState(UploadState.ERROR, exception.message ?: "Ошибка получения ссылки")
            }
        }.addOnFailureListener { exception ->
            updateUIState(UploadState.ERROR, exception.message ?: "Ошибка загрузки файла")
        }
    }

    private fun saveBookToFirestore(
        fileName: String,
        fileType: String,
        fileUrl: String,
        fileSize: Long,
        userId: String
    ) {
        val title = binding.etTitle.text.toString().trim()
        val author = binding.etAuthor.text.toString().trim()

        val book = Book(
            title = title,
            author = author,
            fileUrl = fileUrl,
            userId = auth.currentUser!!.uid
        )

        firestore.collection("books")
            .document(book.title)
            .set(book)
            .addOnSuccessListener {
                updateUIState(UploadState.SUCCESS)
                resetForm()
            }
            .addOnFailureListener { exception ->
                updateUIState(UploadState.ERROR, exception.message ?: "Ошибка сохранения метаданных")
            }
    }

    private fun getFileType(fileName: String): String {
        return when {
            fileName.endsWith(".txt", ignoreCase = true) -> "txt"
            fileName.endsWith(".epub", ignoreCase = true) -> "epub"
            fileName.endsWith(".pdf", ignoreCase = true) -> "pdf"
            else -> "unknown"
        }
    }

    private fun validateInput(): Boolean {
        val title = binding.etTitle.text.toString().trim()
        val author = binding.etAuthor.text.toString().trim()

        // Сбрасываем ошибки
        binding.etTitle.error = null
        binding.etAuthor.error = null

        return when {
            selectedFileUri == null -> {
                Toast.makeText(this, "Выберите файл книги", Toast.LENGTH_SHORT).show()
                false
            }
            title.isEmpty() -> {
                binding.etTitle.error = "Введите название книги"
                false
            }
            author.isEmpty() -> {
                binding.etAuthor.error = "Введите автора"
                false
            }
            else -> true
        }
    }

    private fun updateUIState(state: UploadState, errorMessage: String? = null) {
        when (state) {
            UploadState.IDLE -> {
                binding.progressBar.visibility = android.view.View.GONE
                binding.tvProgress.visibility = android.view.View.GONE
                binding.tvStatus.visibility = android.view.View.GONE
                binding.btnRetry.visibility = android.view.View.GONE
                binding.btnUpload.isEnabled = true
                binding.btnSelectFile.isEnabled = true
            }
            UploadState.UPLOADING -> {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.tvProgress.visibility = android.view.View.VISIBLE
                binding.tvStatus.visibility = android.view.View.GONE
                binding.btnRetry.visibility = android.view.View.GONE
                binding.btnUpload.isEnabled = false
                binding.btnSelectFile.isEnabled = false
                binding.tvStatus.text = "Загрузка..."
            }
            UploadState.SUCCESS -> {
                binding.progressBar.visibility = android.view.View.GONE
                binding.tvProgress.visibility = android.view.View.GONE
                binding.tvStatus.visibility = android.view.View.VISIBLE
                binding.btnRetry.visibility = android.view.View.GONE
                binding.btnUpload.isEnabled = true
                binding.btnSelectFile.isEnabled = true
                binding.tvStatus.text = "✅ Книга успешно загружена"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            }
            UploadState.ERROR -> {
                binding.progressBar.visibility = android.view.View.GONE
                binding.tvProgress.visibility = android.view.View.GONE
                binding.tvStatus.visibility = android.view.View.VISIBLE
                binding.btnRetry.visibility = android.view.View.VISIBLE
                binding.btnUpload.isEnabled = false
                binding.btnSelectFile.isEnabled = true
                binding.tvStatus.text = "❌ $errorMessage"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        }
    }

    private fun resetForm() {
        selectedFileUri = null
        selectedFileName = ""
        binding.etTitle.text?.clear()
        binding.etAuthor.text?.clear()
        binding.tvSelectedFile.text = "Файл не выбран"

        // Через 3 секунды возвращаем в исходное состояние
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            updateUIState(UploadState.IDLE)
        }, 3000)
    }

    enum class UploadState {
        IDLE, UPLOADING, SUCCESS, ERROR
    }
}
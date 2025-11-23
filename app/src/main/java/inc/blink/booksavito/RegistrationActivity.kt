package inc.blink.booksavito

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import inc.blink.booksavito.databinding.ActivityRegistrationBinding
import inc.blink.booksavito.models.User

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private var mAuth: FirebaseAuth? = null
    private var user: User? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeFirebase(this)
        mAuth = FirebaseAuth.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Пользователь уже авторизован - идем на главный экран
            startActivity(Intent(this, MainActivity::class.java))
        }
        user = User(binding.emailEntry.getText().toString())

        binding.authButton.setOnClickListener {
            val myIntent = Intent(
                this,
                AuthActivity::class.java
            )
            this.startActivity(myIntent)
        }

        binding.buttonRegistration.setOnClickListener{
            registerUser()
        }

    }


    /*
        Функция регистрации пользователя
        При успешной авторизации в firebase заносится в базу Firestore
     */
    private fun registerUser() {
        val db = Firebase.firestore
        if (binding.passeordEntry.getText() != null && binding.emailEntry.getText() != null && user!!.isStrictValid(this, binding.emailEntry.getText().toString()) && binding.passeordEntry.text.toString().length <= 8)
         {

            mAuth!!.createUserWithEmailAndPassword(
                binding.emailEntry.text.toString(),
                binding.passeordEntry.text.toString()
            ).addOnCompleteListener(
                OnCompleteListener<AuthResult> { task ->
                    if (task.isSuccessful) {
                        val user: User = Mkusers(
                            binding.emailEntry.getText().toString(), task.result.user!!.uid)
                        db.collection("users")
                            .add(user)
                            .addOnSuccessListener { documentReference ->
                               showToast(this,"Документ добавлен с ID: ${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                showToast(this,"Ошибка добавления: ${e.message}")
                            }
                        val myIntent = Intent(
                            this,
                            UploadBooksActivity::class.java
                        )
                        this.startActivity(myIntent)
                        finish()
                    }
                })
        }
    }


    private fun Mkusers(email: String, uid:String): User {
        return User( email, uid, "")
    }

}
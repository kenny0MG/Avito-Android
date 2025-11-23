package inc.blink.booksavito

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import inc.blink.booksavito.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var mAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeFirebase(this)

        mAuth = FirebaseAuth.getInstance()
        binding.regTextView.setOnClickListener {
            val myIntent = Intent(
                this,
                RegistrationActivity::class.java
            )
            this.startActivity(myIntent)
            finish()
        }
        binding.buttonAuth.setOnClickListener {
            Auth()
        }
    }


     fun Auth() {

             mAuth = FirebaseAuth.getInstance()
             if (!binding.passeordAuth.getText().toString().isEmpty() && !binding.emailAuth.getText().toString().isEmpty()
             ) {
                 mAuth!!.signInWithEmailAndPassword(
                     binding.emailAuth.getText().toString(),
                     binding.passeordAuth.getText().toString()
                 ).addOnCompleteListener { task ->
                     if (task.isSuccessful) {
                         val myIntent = Intent(
                             this,
                             UploadBooksActivity::class.java
                         )
                         this.startActivity(myIntent)
                     }
                 }.addOnFailureListener{
                     showToast(this, it.message.toString())
                 }
             } else {
                 Toast.makeText(this, "Введите Email или пароль", Toast.LENGTH_SHORT).show()
             }

    }



}
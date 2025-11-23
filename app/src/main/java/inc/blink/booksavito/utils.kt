package inc.blink.booksavito

import android.content.Context
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

 fun initializeFirebase(context: Context) {
    try {
        FirebaseApp.initializeApp(context)

    } catch (e: IllegalStateException) {
        // Уже инициализирован - это нормально
    }
}
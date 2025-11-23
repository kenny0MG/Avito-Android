package inc.blink.booksavito.models

import android.content.Context
import inc.blink.booksavito.showToast

//решулярные выражения для валидация email
private const val STRICT_EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"

data class User(val email: String, val uid: String = "", val photo: String = "", val books: Map<String, String> = emptyMap()){




        fun isStrictValid(context: Context, email: String): Boolean {
            if(!email.matches(STRICT_EMAIL_REGEX.toRegex())){
                showToast(context,"Не верно введена почта")
                return false
            }
            return email.matches(STRICT_EMAIL_REGEX.toRegex())
        }



}


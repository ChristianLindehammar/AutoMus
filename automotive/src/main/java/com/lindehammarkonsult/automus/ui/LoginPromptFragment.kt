package com.lindehammarkonsult.automus.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.lindehammarkonsult.automus.MainActivity
import com.lindehammarkonsult.automus.R

/**
 * Login prompt fragment shown when the user is not authenticated
 */
class LoginPromptFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_prompt, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up sign in button click listener
        view.findViewById<Button>(R.id.btn_sign_in).setOnClickListener {
            // Start the login flow using the activity
            (activity as? MainActivity)?.startAppleMusicLogin()
        }
        
        // Set up long press on login button to launch test activity
        view.findViewById<Button>(R.id.btn_sign_in).setOnLongClickListener {
            // Start the authentication test activity
            val intent = android.content.Intent(requireContext(), com.lindehammarkonsult.automus.AppleMusicAuthTestActivity::class.java)
            startActivity(intent)
            true
        }
    }
    
    companion object {
        fun newInstance(): LoginPromptFragment {
            return LoginPromptFragment()
        }
    }
}

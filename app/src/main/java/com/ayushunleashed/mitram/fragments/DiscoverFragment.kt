package com.ayushunleashed.mitram.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import com.asynctaskcoffee.cardstack.CardContainer
import com.asynctaskcoffee.cardstack.CardListener
import com.asynctaskcoffee.cardstack.pulse
import com.asynctaskcoffee.cardstack.px
import com.ayushunleashed.mitram.FragmentHomeActivity
import com.ayushunleashed.mitram.R
import com.ayushunleashed.mitram.adapters.UserCardAdapter
import com.ayushunleashed.mitram.models.UserModel
import com.bumptech.glide.Glide
import com.firebase.ui.auth.data.model.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.lorentzos.flingswipe.SwipeFlingAdapterView
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.ArrayList


class DiscoverFragment : Fragment() ,CardListener{

    lateinit var cardContainer: CardContainer
    lateinit var userCardAdapter: UserCardAdapter
    lateinit var usersList:MutableList<UserModel>
    lateinit var currentUser: FirebaseUser
    lateinit var db:FirebaseFirestore
    lateinit var userImage:ImageView
    lateinit var btnReloadDiscoverUsers: FloatingActionButton
    lateinit var btnRightSwipe:FloatingActionButton
    lateinit var btnLeftSwipe:FloatingActionButton

//    lateinit var myToolbarDiscover:androidx.appcompat.widget.Toolbar
//    lateinit var custom_title:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        setupViews(view)

        /*Customization*/
        cardContainer.maxStackSize = 1

        cardContainer.setOnCardActionListener(this)
        currentUser = FirebaseAuth.getInstance().currentUser!!
        db  = FirebaseFirestore.getInstance()

        btnReloadDiscoverUsers.setOnClickListener{
            handleReloadUsersButton()
        }

        btnRightSwipe.setOnClickListener{
            it.pulse() // for sweet animation
            userCardAdapter.swipeRight()

        }
        btnLeftSwipe.setOnClickListener {
            it.pulse() // for sweet animation
            userCardAdapter.swipeLeft()
        }


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //customToolBar(view)
        loadUserImage()
        loadData()

    }

    fun loadUserImage()
    {
        GlobalScope.launch(Dispatchers.IO) {
            val user = db.collection("users").document(currentUser.uid).get().await().toObject(UserModel::class.java)

            withContext(Dispatchers.Main)
            {
                if (user != null) {
                    Glide.with(userImage.context).load(user.imageUrl).circleCrop().placeholder(R.drawable.img_user_place_holder)
                        .error(R.drawable.img_user_profile_sample).into(userImage)
                }
            }
        }

    }

    fun loadData()
    {        val db = FirebaseFirestore.getInstance()
            GlobalScope.launch(Dispatchers.IO){

            //get all users from database
            val users = async{db.collection("users").whereNotEqualTo("uid",currentUser.uid).get().await().toObjects(UserModel::class.java)}

//            // get current user model from database
//            val currentUserModel = db.collection("users").document(currentUser.uid).get().await().toObject(UserModel::class.java)

            // delete current user from the list of all users.
//            users.await().remove(currentUserModel)
//                if(users.await().contains(currentUserModel))
//                {
//                    if (currentUserModel != null) {
//                        Log.d("GENERAL","discover screen still contains"+currentUserModel.displayName)
//                    };
//                }

                // adding users to the userslist
            usersList = users.await()

                // updating ui with users
            withContext(Dispatchers.Main)
            {
                if(usersList.size == 0)
                {
                    Toast.makeText(requireContext(),"No Users To Show",Toast.LENGTH_SHORT).show()
                }


                Log.d("GENERAL","${usersList.size} users in Discover");
                for(user in usersList)
                {
                    Log.d("GENERAL","**user:"+user.displayName.toString());
                }

                userCardAdapter = UserCardAdapter(usersList,requireContext())
                cardContainer.setAdapter(userCardAdapter)
            }
        }
    }

    fun setupViews(view: View)
    {
        btnLeftSwipe = view.findViewById(R.id.btnLeftSwipe)
        btnRightSwipe = view.findViewById(R.id.btnRightSwipe)
        cardContainer = view.findViewById(R.id.cardContainer)
        userImage = view.findViewById(R.id.userImage)
        btnReloadDiscoverUsers = view.findViewById<FloatingActionButton>(R.id.btnReloadDiscoverUsers)
    }

    fun defaultProfiles():MutableList<UserModel>
    {
        var usersList = mutableListOf<UserModel>(
            UserModel("001","Ayush Yadav","https://via.placeholder.com/300","Def"),
            UserModel("002","Khushboo pratap singh","https://via.placeholder.com/300","Not Def"),
            UserModel("003","Yuvraj Singh","https://via.placeholder.com/300","depressed"),
            UserModel("004","Ramraj Gopal","https://via.placeholder.com/300","I  am legned"),
            UserModel("005","Kishor Kumar Singh","https://via.placeholder.com/300","Kishor OP"),
            UserModel("006","Kumar Shrivastava","https://via.placeholder.com/300","hi buddy"),
            UserModel("007","Rob Stark","https://via.placeholder.com/300","kuch bhi P"),
            UserModel("008","John snow","https://via.placeholder.com/300","John hun me")
        )

        return usersList;
    }

    override fun onLeftSwipe(position: Int, model: Any) {
        Log.e(
            "SwipeLog",
            "onLeftSwipe pos: $position model: " + (model as UserModel).toString()
        )
        Toast.makeText(requireContext(),"Left Swiped",Toast.LENGTH_SHORT).show()
    }

    override fun onRightSwipe(position: Int, model: Any) {
        Log.e(
            "SwipeLog",
            "onRightSwipe pos: $position model: " + (model as UserModel).toString()
        )

        val userWhoGotRightSwiped:UserModel = model as UserModel

        if(!userWhoGotRightSwiped.likedBy.contains(currentUser.uid) && !userWhoGotRightSwiped.connections.contains(currentUser.uid))
        {
            userWhoGotRightSwiped.likedBy.add(currentUser.uid)
            Log.e(
                "SwipeLog",
                "onRightSwipe pos: $position model: " + (model as UserModel).toString()
            )

            GlobalScope.launch(Dispatchers.IO) {
                db.collection("users").document(userWhoGotRightSwiped.uid).set(userWhoGotRightSwiped).
                addOnSuccessListener {
                    Toast.makeText(requireContext(),"Like request Sent", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(),"Failed To sent like request", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else
        {
            Toast.makeText(requireContext(),"Already Exist not adding",Toast.LENGTH_SHORT).show()
        }

    }

    override fun onItemShow(position: Int, model: Any) {
        Log.e("SwipeLog", "onItemShow pos: $position model: " + (model as UserModel).toString())
    }

    override fun onSwipeCancel(position: Int, model: Any) {
        Log.e(
            "SwipeLog",
            "onSwipeCancel pos: $position model: " + (model as UserModel).toString()
        )
    }

    override fun onSwipeCompleted() {
        Log.e(
            "SwipeLog",
            "Out of swipe data"
        )
        btnReloadDiscoverUsers.visibility = View.VISIBLE
        btnLeftSwipe.visibility = View.GONE
        btnRightSwipe.visibility = View.GONE
    }

    fun handleReloadUsersButton()
    {
        //loadData()
        usersList = defaultProfiles()
        userCardAdapter = UserCardAdapter(usersList,requireContext())
        cardContainer.setAdapter(userCardAdapter)
        btnReloadDiscoverUsers.visibility = View.GONE
        btnLeftSwipe.visibility = View.VISIBLE
        btnRightSwipe.visibility = View.VISIBLE
    }
}
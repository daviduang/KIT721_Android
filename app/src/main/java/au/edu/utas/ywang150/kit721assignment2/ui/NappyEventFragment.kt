package au.edu.utas.ywang150.kit721assignment2.ui

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import au.edu.utas.ywang150.kit721assignment2.FIREBASE_TAG
import au.edu.utas.ywang150.kit721assignment2.MainActivity
import au.edu.utas.ywang150.kit721assignment2.R
import au.edu.utas.ywang150.kit721assignment2.databinding.FragmentNappyEventBinding
import au.edu.utas.ywang150.kit721assignment2.ui.models.NappyEvent
import au.edu.utas.ywang150.kit721assignment2.ui.modules.OptionButtonModule
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Nappy event creation and edition fragment
 */
class NappyEventFragment : Fragment() {

    private var nappyEvent: NappyEvent? = null

    private var imageUrl = String()

    // Create image file path
    private var currentPhotoPath = String()
    private var currentPhotoURI = String()

    // View binding
    private var _ui: FragmentNappyEventBinding? = null
    private val ui get() = _ui!!

    // Get camera permission
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionResult ->
        if (permissionResult) {
            takeAPicture()
        } else {
            Toast.makeText(requireContext(), "Camera Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Get camera result and set the picture
    private val getCameraResult = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {cameraResult ->
        if (cameraResult) {
            setPic(ui.imageView)
        }
    }

    // Get gallery permission
    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Gallery Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Retrieve the selected image
    private val pickImageResult = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentPhotoURI = uri.toString()
            ui.imageView.setImageURI(uri)
        }
    }

    private fun openGallery() {
        pickImageResult.launch("image/*")
    }

    // Pass data into this fragment for editing view (From ChatGPT)
    companion object {
        private const val ARG_NAPPY_EVENT = "arg_nappy_event"

        fun newInstance(nappyEvent: NappyEvent? = null): NappyEventFragment {
            val args = Bundle()
            args.putParcelable(ARG_NAPPY_EVENT, nappyEvent)
            val fragment = NappyEventFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // View binding (From ChatGPT)
        _ui = FragmentNappyEventBinding.inflate(inflater, container, false)
        return ui.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the passed data (From ChatGPT)
        if (arguments != null) {
            nappyEvent = requireArguments().getParcelable(ARG_NAPPY_EVENT)!!
        }
        nappyEvent?.let { event ->

            // Update UI with event values
            val type = event.type
            val note = event.note
            imageUrl = event.imageUrl.toString()

            // Update UI elements

            // Set Change Nappy option button
            when(type) {
                "Wet" -> {
                    ui.btnWet.isSelected = true
                }
                "Wet Dirty" -> {
                    ui.btnWetDirty.isSelected = true
                }
            }

            // Set image using Glide (From ChatGPT)
            if (imageUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(imageUrl)
                    .into(ui.imageView)

                // Show the image view
                ui.imageView.visibility = View.VISIBLE
            }

            // Update optional note
            ui.nappyNote.setText(note)
            Log.d("Nappy event", event.type.toString())

        }

        // Set Go Back button
        (activity as MainActivity).setButtonClickListener(R.id.btn_back) {
            parentFragmentManager.popBackStack()
        }

        // Set Nappy option module
        val options = OptionButtonModule(view, listOf(R.id.btn_wet, R.id.btn_wet_dirty))

        // Set Camera button
        ui.btnCamera.setOnClickListener{
            requestTakeAPicture()
            ui.imageView.visibility = View.VISIBLE
        }

        // Set Gallery button
        ui.btnGallery.setOnClickListener {
            requestStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            ui.imageView.visibility = View.VISIBLE
        }

        // Set submit button
        (activity as MainActivity).setButtonClickListener(R.id.btn_done) {

            // Retrieve nappy type selected by user
            val selectedOption = options.getSelectedButton()

            // Retrieve input text from optional note field
            val noteText: EditText = view.findViewById(R.id.nappy_note)
            val inputNote: String = noteText.text.toString()

            // If no nappy type selection was made by user, show this alert message
            if (selectedOption == null) {
                val alertDialogBuilder = AlertDialog.Builder(requireContext())
                alertDialogBuilder.setTitle("Please select a nappy type!")
                alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()
            }

            else {

                // Covert photo uri or photo path to storage uri based on gallery or camera photo
                var storageUri: Uri?
                if (currentPhotoURI.isNotEmpty()) {
                    storageUri = Uri.parse(currentPhotoURI)
                } else {
                    storageUri = Uri.fromFile(File(currentPhotoPath))
                }

                // get db connection
                val db = Firebase.firestore
                Log.d("FIREBASE", "Firebase connected: ${db.app.name}")

                // If there are some data passed in this fragment, then update the db instead of create
                if (arguments != null) {

                    // Get the document
                    val nappyEventCollection = nappyEvent?.id?.let { it1 ->
                        db.collection("events").document(it1)
                    }

                    uploadImageToFirebaseStorage(storageUri, nappyEvent?.id) { imageUrl ->
                        val updates = hashMapOf<String, Any>(
                            "type" to selectedOption?.text.toString(),
                            "note" to inputNote,
                            "imageUrl" to imageUrl
                        )

                        // Update the database
                        nappyEventCollection?.update(updates)
                            ?.addOnSuccessListener {
                                Log.d("Firestore", "Document successfully updated!")

                                // Show a toast message to indicate that the document has been updated
                                Toast.makeText(requireContext(),
                                    "Nappy Event has been updated!", Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnFailureListener { e ->
                                Log.w("Firestore", "Error updating document", e)
                            }
                    }

                } else {

                    uploadImageToFirebaseStorage(storageUri) { imageUrl ->
                        val newNappyEvent = NappyEvent(
                            type = selectedOption?.text.toString(),
                            imageUrl = imageUrl,
                            note = inputNote
                        )

                        val nappyEventCollection = db.collection("events")

                        nappyEventCollection
                            .add(newNappyEvent)
                            .addOnSuccessListener {
                                Log.d(FIREBASE_TAG, "Document created with id ${it.id}")
                                newNappyEvent.id = it.id

                                // Show a toast message to indicate that the document has been created
                                Toast.makeText(requireContext(),
                                    "Nappy Event has been created!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Log.e(FIREBASE_TAG, "Error writing document", it)
                            }
                    }
                }
            }
        }

    }

    // Request the permission for Camera
    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestTakeAPicture(){
        requestPermission.launch(android.Manifest.permission.CAMERA)
    }

    // Take a picture function
    private fun takeAPicture() {
        val photoFile: File = createImageFile()!!
        val photoURI: Uri = FileProvider.getUriForFile(
            requireContext(),
            "au.edu.utas.ywang150.kit721assignment2",
            photoFile)
        getCameraResult.launch(photoURI)
    }

    // Set the picture to an image view
    private fun setPic(imageView: ImageView) {

        // Get the dimensions of the View
        val targetW: Int = imageView.width
        val targetH: Int = imageView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)
            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.
        also { bitmap ->imageView.setImageBitmap(bitmap)}
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply { // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // Upload image by image uri
    private fun uploadImageToFirebaseStorage(fileUri: Uri, documentId: String? = null, onSuccess: (imageUrl: String) -> Unit) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageName = if (documentId != null) "images/${documentId}.jpg" else "images/$timeStamp.jpg"
        val imageRef = storageRef.child(imageName)

        // Check if file Uri is empty
        if (fileUri.toString().isEmpty()) {
            Log.e(FIREBASE_TAG, "Error: fileUri is empty")
            return
        }

        // Upload image data stream to fire store (From ChatGPT)
        val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(fileUri)
        if (inputStream != null) {
            val uploadTask = imageRef.putStream(inputStream)
            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onSuccess(downloadUrl.toString())
                }
            }.addOnFailureListener { exception ->
                Log.e(FIREBASE_TAG, "Error uploading image", exception)
            }
        } else {
            Log.e(FIREBASE_TAG, "Error: Input stream is null")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Disable the customized action bar when leaving this fragment
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowCustomEnabled(false)
    }
}
package com.simplycarfleet.nav_menu

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.simplycarfleet.R
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.databinding.FragmentAddCarBinding
import com.google.firebase.Timestamp
import com.simplycarfleet.functions.FunctionsAndValues
import java.io.ByteArrayOutputStream
import kotlin.collections.HashMap

class AddCarFragment : FunctionsAndValues() {
    // Zmienne binding
    private var _binding: FragmentAddCarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Usunięcie ActionBara we fragmencie
        (activity as MainActivity?)?.supportActionBar?.hide()

        binding.buttonCancelAddCar.setOnClickListener {
            // Przekieruj do poprzedniego fragmentu
            replaceFragment(CarsFragment())
        }

        binding.buttonConfirmAddCar.setOnClickListener {
            // Dodaj nowy pojazd
            addNewCar()
        }

        // Wstawienie defaultowego zdjęcia samochodu
        Glide.with(view)
            .load("https://firebasestorage.googleapis.com/v0/b/simply-carfleet.appspot.com/o/cars%2Fdefault_car_image.jpg?alt=media&token=c1df9c53-54c5-4529-a9c6-87d46535d90d")
            .into(binding.carPhotoImageView)

        binding.carPhotoImageView.setOnClickListener {
            // Wybierz zdjęcie
            ImagePicker.with(this)
                .compress(1024) // Całkowita waga pliku będzie mniejsza niz 1 MB
                .crop(16f, 9f) // Kadrowanie w proporcjach 16:9
                .maxResultSize(1080,
                    1080)  // Ostateczna rozdzielczość pliku będzie wynosić 1080x1080
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        // W przypadku kliknięcia przycisku wstecz przekieruj do poprzedniego fragmentu
        requireView().setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                replaceFragment(CarsFragment())
                true
            } else false
        }
    }


    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Image Uri nie będzie nullem dla RESULT_OK
                    val fileUri = data?.data!!

                    // Ustawianie podglądu zdjęcia
                    binding.carPhotoImageView.setImageURI(fileUri)
                }
                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(context, getString(R.string.add_car_cancel_pick_photo), Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun addNewCar() {
        // Zmienne pobierające input użytkownika
        val brand = binding.carBrandInput.text.toString()
        val yearOfProd = binding.carYearOfProdInput.text.toString()
        val model = binding.carModelInput.text.toString()
        val vinNumber = binding.carVinInput.text.toString()
        val fuelType = binding.carFuelTypeInput.text.toString()
        val policyNumber =
            binding.carPolicyNumberInput.text.toString()
        val notes = binding.carNotesInput.text.toString()
        val id = System.currentTimeMillis()
            .toString() // Generowanie losowego ID pojazdu jako aktualny czas podany w milisekundach

        // Walidacja podstawowych pól
        if (brand.isNotEmpty() && yearOfProd.isNotEmpty() && model.isNotEmpty() && fuelType.isNotEmpty()) {
            val car: MutableMap<String, Any> = HashMap()
            car["brand"] = brand
            car["yearOfProduction"] = yearOfProd
            car["model"] = model
            car["VIN"] = vinNumber
            car["fuelType"] = fuelType
            car["policyNumber"] = policyNumber
            car["notes"] = notes
            car["id"] = id
            car["dateCreated"] = Timestamp.now()

            cloud.collection("users").document(uid.toString()).collection("cars").document(id)
                .set(car)
                .addOnSuccessListener {
                    Toast.makeText(
                        activity,
                        getString(R.string.add_car_successfully_added_new_car, brand),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Konwersja zdjęcia na bitmapę
                    val bitmap = (binding.carPhotoImageView.drawable as BitmapDrawable).bitmap
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data = baos.toByteArray()

                    // Zapisz zdjęcie w bazie danych
                    uploadCarPhoto(data, id, "carImageId", "yyyy_MM_dd_HH_mm_ss", "CarPhotos" )

                    // W przypadku pomyślnego dodania pojazdu przekieruj do poprzedniego fragmentu
                    replaceFragment(CarsFragment())
                }
                .addOnFailureListener {
                    Toast.makeText(activity, getString(R.string.add_car_failure_message), Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(
                activity,
                getString(R.string.add_car_fill_all_fields),
                Toast.LENGTH_LONG
            ).show() //tu sie doda wyswietlanie gwiazdki, wymaganie pól i skrócenie Toasta
        }
    }
}
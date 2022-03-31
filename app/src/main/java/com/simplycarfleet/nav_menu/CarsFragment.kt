package com.simplycarfleet.nav_menu

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.firestore.DocumentChange
import com.simplycarfleet.R
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.data.Car
import com.simplycarfleet.databinding.FragmentCarsBinding
import com.simplycarfleet.functions.FunctionsAndValues
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList

class CarsFragment : FunctionsAndValues() {
    // Zmienne binding
    private var _binding: FragmentCarsBinding? = null
    private val binding get() = _binding!!


    // Inne zmienne
    private val repoDebug = "REPO_DEBUG"
    private lateinit var carList: ArrayList<Car>
    private lateinit var carAdapter: CarAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCarsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = binding.recyclerViewCars
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        carList = arrayListOf()
        carAdapter = CarAdapter(carList)
        recyclerView.adapter = carAdapter
        carAdapter.setOnItemClickListener(
            object : CarAdapter.OnItemClickListener {
                override fun onItemClick(position: Int, car: Car) {
                    carShowMoreInfo(
                        car.brand.toString(),
                        car.model.toString(),
                        car.yearOfProduction.toString(),
                        car.fuelType.toString(),
                        car.VIN.toString(),
                        car.policyNumber.toString(),
                        car.notes.toString(),
                        car.carImageId.toString()
                    )
                }
            },
        )

        carAdapter.setOnCarMoreOptionsClickListener(
            object : CarAdapter.OnCarMoreOptionsClickListener {
                override fun onItemClick(position: Int, car: Car) {
                    saveDataToSharedPreferences(requireContext(), car.id.toString(), "CAR ID")

                    val newView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
                    popupMenus(
                        newView!!,
                        car.id.toString(),
                        car.brand.toString(),
                        car.model.toString(),
                        car.yearOfProduction.toString(),
                        car.fuelType.toString(),
                        car.VIN.toString(),
                        car.policyNumber.toString(),
                        car.notes.toString()
                    )
                }
            },
        )
        realTimeUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        (activity as MainActivity?)?.supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.add_car_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_car -> replaceFragment(AddCarFragment())
        }
        return super.onOptionsItemSelected(item)
    }


    private fun realTimeUpdates() {
        val cars = cloud.collection("users").document(uid.toString()).collection("cars")
        cars.addSnapshotListener { querySnapshot, error ->
            error?.let {
                Log.w(TAG, "Listen failed", error)
                return@addSnapshotListener
            }
            for (dc: DocumentChange in querySnapshot?.documentChanges!!) {
                if (dc.type == DocumentChange.Type.ADDED) {
                    carList.add(dc.document.toObject(Car::class.java))
                }

                if (dc.type == DocumentChange.Type.REMOVED) {
                    carList.remove(dc.document.toObject(Car::class.java))
                }

                if (dc.type == DocumentChange.Type.MODIFIED) {
                    if (dc.oldIndex == dc.newIndex) {
                        carList.removeAt(dc.oldIndex)
                        carList.add(dc.oldIndex, dc.document.toObject(Car::class.java))
                        carAdapter.notifyItemMoved(dc.oldIndex, dc.newIndex)
                    }
                }
            }
            carAdapter.notifyDataSetChanged()
            // Jeżeli lista pojazdów jest równa 0 pokaż stosowny komunikat
            if (carList.size == 0) binding.fragmentCarsNoEntriesCard.visibility =
                View.VISIBLE
            // W przeciwnym wypadku pokaż wpisy i ukryj komunikat
            else binding.fragmentCarsNoEntriesCard.visibility = View.GONE
        }
    }

    private fun carShowMoreInfo(
        brand: String,
        model: String,
        yearOfProduction: String,
        fuelType: String,
        VIN: String,
        policyNumber: String,
        notes: String,
        carImageId: String,
    ) {
        val car: MutableMap<String, Any> = HashMap()
        car["brand"] = brand
        car["model"] = model
        car["yearOfProduction"] = yearOfProduction
        car["fuelType"] = fuelType
        car["VIN"] = VIN
        car["policyNumber"] = policyNumber
        car["notes"] = notes

        val inflateCar = LayoutInflater.from(context)
        val v = inflateCar.inflate(R.layout.car_more_info_dialog, null)

        val addDialog = AlertDialog.Builder(context)
        val carPhotoImageView = v.findViewById<ImageView>(R.id.car_photo_image_view_dialog)

        // Pobranie wartości z bazy danych i wstawienie ich do podglądu
        v.findViewById<EditText>(R.id.car_brand_edittext_dialog).setText(brand)
        v.findViewById<EditText>(R.id.car_model_edittext_dialog).setText(model)
        v.findViewById<EditText>(R.id.car_year_of_prod_edittext_dialog).setText(yearOfProduction)
        v.findViewById<EditText>(R.id.car_fuel_type_edittext_dialog).setText(fuelType)
        v.findViewById<EditText>(R.id.car_vin_edittext_dialog).setText(VIN)
        v.findViewById<EditText>(R.id.car_policy_number_edittext_dialog).setText(policyNumber)
        v.findViewById<EditText>(R.id.car_notes_edittext_dialog).setText(notes)

        // Wyświetlanie zdjęcia pojazdu
        Glide.with(v)
            .load(carImageId)
            .into(carPhotoImageView)

        // Tworzenie dialogu
        addDialog.setView(v)
        addDialog.setNegativeButton(getString(R.string.cars_dialog_negative_button_close)) { dialog, _ ->
            AlertDialog.Builder(context)
            dialog.dismiss()
        }
        addDialog.create()
        addDialog.show()
    }

    private fun delCar(id: String) {
        val car: MutableMap<String, Any> = HashMap()
        car["id"] = id

        // Usunięcie pojazdu o podanym ID
        cloud.collection("users").document(uid.toString()).collection("cars").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    getString(R.string.cars_delete_car_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    getString(R.string.cars_delete_car_failure),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun modifyCar(
        id: String,
        brand: String,
        model: String,
        yearOfProduction: String,
        fuelType: String,
        VIN: String,
        policyNumber: String,
        notes: String,
    ) {
        val car: MutableMap<String, Any> = HashMap()
        car["id"] = id
        car["brand"] = brand
        car["model"] = model
        car["yearOfProduction"] = yearOfProduction
        car["fuelType"] = fuelType
        car["VIN"] = VIN
        car["policyNumber"] = policyNumber
        car["notes"] = notes

        val inflateCar = LayoutInflater.from(context)
        val v = inflateCar.inflate(R.layout.modify_car_layout, null)
        val addDialog = AlertDialog.Builder(context)

        // Pobranie wartości z bazy danych i wstawienie ich do podglądu
        v.findViewById<EditText>(R.id.car_modify_brand_edittext_dialog).setText(brand)
        v.findViewById<EditText>(R.id.car_modify_model_edittext_dialog).setText(model)
        v.findViewById<EditText>(R.id.car_modify_year_of_prod_edittext_dialog)
            .setText(yearOfProduction)
        v.findViewById<EditText>(R.id.car_modify_fuel_type_edittext_dialog).setText(fuelType)
        v.findViewById<EditText>(R.id.car_modify_vin_edittext_dialog).setText(VIN)
        v.findViewById<EditText>(R.id.car_modify_policy_number_edittext_dialog)
            .setText(policyNumber)
        v.findViewById<EditText>(R.id.car_modify_notes_edittext_dialog).setText(notes)

        // Tworzenie dialogu
        addDialog.setView(v)
        addDialog.setPositiveButton(getString(R.string.cars_dialog_positive_button_edit)) { dialog, _ ->
            val modifyBrand =
                v.findViewById<EditText>(R.id.car_modify_brand_edittext_dialog).text.toString()
            val modifyModel =
                v.findViewById<EditText>(R.id.car_modify_model_edittext_dialog).text.toString()
            val modifyYearOfProd =
                v.findViewById<EditText>(R.id.car_modify_year_of_prod_edittext_dialog).text.toString()
            val modifyFuelType =
                v.findViewById<EditText>(R.id.car_modify_fuel_type_edittext_dialog).text.toString()
            val modifyVIN =
                v.findViewById<EditText>(R.id.car_modify_vin_edittext_dialog).text.toString()
            val modifyPolicyNumber =
                v.findViewById<EditText>(R.id.car_modify_policy_number_edittext_dialog).text.toString()
            val modifyNotes =
                v.findViewById<EditText>(R.id.car_modify_notes_edittext_dialog).text.toString()

            if (modifyBrand.isNotEmpty()
                && modifyModel.isNotEmpty()
                && modifyYearOfProd.isNotEmpty()
                && modifyFuelType.isNotEmpty()
                && modifyVIN.isNotEmpty()
                && modifyPolicyNumber.isNotEmpty()
                && modifyNotes.isNotEmpty()
            ) {
                cloud.collection("users").document(uid.toString()).collection("cars").document(id)
                    .update(
                        mapOf(
                            "brand" to modifyBrand,
                            "model" to modifyModel,
                            "yearOfProduction" to modifyYearOfProd,
                            "fuelType" to modifyFuelType,
                            "VIN" to modifyVIN,
                            "policyNumber" to modifyPolicyNumber,
                            "notes" to modifyNotes
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(
                            context,
                            getString(R.string.cars_modify_car_success),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            getString(R.string.cars_modify_car_failure),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.cars_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }
        addDialog.setNegativeButton(getString(R.string.cars_dialog_negative_button_cancel)) { dialog, _ ->
            AlertDialog.Builder(context)
            dialog.dismiss()
        }
        addDialog.create()
        addDialog.show()
    }

    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    //Image Uri nie będzie nullem dla RESULT_OK
                    val fileUri = data?.data!!
                    binding.carModifyPhotoImageView.setImageURI(fileUri)

                    // Konwersja na bitmapę
                    val bitmap = (binding.carModifyPhotoImageView.drawable as BitmapDrawable).bitmap
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val imageData = baos.toByteArray()

                    // Pobranie ID pojazdu do zmiany zdjęcia
                    val preferences: SharedPreferences =
                        requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)

                    val carId = preferences.getString("CAR ID", null).toString()

                    //uploadCarPhoto(imageData, carId)
                    uploadCarPhoto(
                        imageData,
                        carId,
                        "carImageId",
                        "yyyy_MM_dd_HH_mm_ss",
                        "CarPhotos"
                    )


                    Toast.makeText(
                        context,
                        getString(R.string.cars_photo_changed_successfully),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(
                        context,
                        getString(R.string.cars_photo_change_canceled),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private fun popupMenus(
        v: View,
        carId: String,
        brand: String,
        model: String,
        yearOfProduction: String,
        fuelType: String,
        VIN: String,
        policyNumber: String,
        notes: String,
    ) {
        val popupMenus = PopupMenu(context, v, Gravity.END, R.attr.actionOverflowMenuStyle, 0)
        popupMenus.inflate(R.menu.menu_cars)
        popupMenus.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.modify_car -> {
                    modifyCar(
                        carId,
                        brand,
                        model,
                        yearOfProduction,
                        fuelType,
                        VIN,
                        policyNumber,
                        notes
                    )
                    true
                }
                R.id.change_photo -> {

                    // Wybierz zdjęcie
                    ImagePicker.with(context as MainActivity)
                        .compress(1024) // Całkowita waga pliku będzie mniejsza niz 1 MB
                        .crop(16f, 9f) // Kadrowanie w proporcjach 16:9
                        .maxResultSize(
                            1080,
                            1080
                        )  // Ostateczna rozdzielczość pliku będzie wynosić 1080x1080
                        .createIntent { intent ->
                            startForProfileImageResult.launch(intent)
                        }
                    true
                }
                R.id.del_car -> {
                    AlertDialog.Builder(context)
                        .setTitle(getString(R.string.cars_delete_car_warning_title))
                        .setIcon(R.drawable.ic_baseline_warning_24)
                        .setMessage(getString(R.string.cars_delete_car_warning_message))
                        .setPositiveButton(getString(R.string.cars_delete_car_warning_positive_button)) { dialog, _ ->
                            delCar(carId)
                            Toast.makeText(
                                context,
                                getString(R.string.cars_delete_car_success),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.cars_delete_car_warning_negative_button)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                    true
                }

                else -> true
            }
        }
        popupMenus.show()
    }


}



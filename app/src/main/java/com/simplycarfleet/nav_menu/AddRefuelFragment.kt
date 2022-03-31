package com.simplycarfleet.nav_menu

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.simplycarfleet.R
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.databinding.FragmentAddRefuelBinding
import com.simplycarfleet.functions.FunctionsAndValues
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class AddRefuelFragment : FunctionsAndValues() {
    // Zmienne binding
    private var _binding: FragmentAddRefuelBinding? = null
    private val binding get() = _binding!!

    // Inne zmienne
    private var lastCarMileage = "0"
    private var lastCarMileageDifference = "0"
    private var todayDateInMillis = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddRefuelBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Usunięcie ActionBara we fragmencie
        (activity as MainActivity?)?.supportActionBar?.hide()
        // W przypadku kliknięcia przycisku anuluj przekieruj do poprzedniego fragmentu
        binding.buttonCancelAddRefuel.setOnClickListener {
            replaceFragment(HomeFragment())
        }

        sharedViewModel.carId.observe(viewLifecycleOwner, { carId ->
            val preferences: SharedPreferences =
                requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
            val unitOfDistance = preferences.getString("unit_of_distance", " km")

            val cloudCarCollectionReference = cloud.collection("users")
                .document(uid.toString())
                .collection("cars")
                .document(carId)
                .collection("Car_statistics")
                .orderBy("statisticDateInMillis", Query.Direction.DESCENDING)
                .limit(1)

            val cloudCarRefuelingCollectionReference = cloud.collection("users")
                .document(uid.toString())
                .collection("cars")
                .document(carId)
                .collection("Car_statistics")
                .whereEqualTo("typeOfStatistic", "Tankowanie")
                .orderBy("statisticDateInMillis", Query.Direction.DESCENDING)
                .limit(1)

            cloudCarCollectionReference.get().addOnCompleteListener { result ->
                fun onComplete(task: Task<QuerySnapshot?>) {
                    if (task.isSuccessful) {
                        for (document in task.result!!) {
                            lastCarMileage = document.getString("carMileage").toString()
                        }
                        binding.refuelCarMileageTextfield.hint =
                            getString(
                                R.string.add_refuel_car_mileage_textfield_hint,
                                lastCarMileage
                            ) + " [${unitOfDistance?.trim()}]"
                    }
                }
                onComplete(result)
            }

            cloudCarRefuelingCollectionReference.get().addOnCompleteListener { result ->
                fun onComplete(task: Task<QuerySnapshot?>) {
                    if (task.isSuccessful) {
                        for (document in task.result!!) {
                            lastCarMileageDifference = document.getString("carMileage").toString()
                        }
                    }
                }
                onComplete(result)
            }
        })

        // Wywołanie funkcji dodającej tankowanie
        addRefuelToFirebase()

        // Wstawienie defaultowego zdjęcia samochodu
        Glide.with(view)
            .load("https://firebasestorage.googleapis.com/v0/b/simply-carfleet.appspot.com/o/statisticsImages%2Freceipt_default_image.png?alt=media&token=8be25f02-e89a-4cbd-96e2-bc28c57bc289")
            .into(binding.addRefuelReceiptImageView)
        Glide.with(view)
            .load("https://firebasestorage.googleapis.com/v0/b/simply-carfleet.appspot.com/o/statisticsImages%2Freceipt_default_image.png?alt=media&token=8be25f02-e89a-4cbd-96e2-bc28c57bc289")
            .into(binding.addRefuelReceiptImageView2)
        Glide.with(view)
            .load("https://firebasestorage.googleapis.com/v0/b/simply-carfleet.appspot.com/o/statisticsImages%2Freceipt_default_image.png?alt=media&token=8be25f02-e89a-4cbd-96e2-bc28c57bc289")
            .into(binding.addRefuelReceiptImageView3)

        binding.addRefuelReceiptImageView.setOnClickListener {
            // Wybierz zdjęcie
            ImagePicker.with(this)
                .compress(1024) // Całkowita waga pliku będzie mniejsza niz 1 MB
                .crop() // Kadrowanie, możliwośc wyboru proporcji przez użytkownika
                .maxResultSize(
                    1080,
                    1080
                )  // Ostateczna rozdzielczość pliku będzie wynosić 1080x1080
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }
        binding.addRefuelReceiptImageView2.setOnClickListener {
            // Wybierz zdjęcie
            ImagePicker.with(this)
                .compress(1024) // Całkowita waga pliku będzie mniejsza niz 1 MB
                .crop() // Kadrowanie, możliwośc wyboru proporcji przez użytkownika
                .maxResultSize(
                    1080,
                    1080
                )  // Ostateczna rozdzielczość pliku będzie wynosić 1080x1080
                .createIntent { intent ->
                    startForProfileImageResult2.launch(intent)
                }
        }
        binding.addRefuelReceiptImageView3.setOnClickListener {
            // Wybierz zdjęcie
            ImagePicker.with(this)
                .compress(1024) // Całkowita waga pliku będzie mniejsza niz 1 MB
                .crop() // Kadrowanie, możliwośc wyboru proporcji przez użytkownika
                .maxResultSize(
                    1080,
                    1080
                )  // Ostateczna rozdzielczość pliku będzie wynosić 1080x1080
                .createIntent { intent ->
                    startForProfileImageResult3.launch(intent)
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
                replaceFragment(HomeFragment())
                true
            } else false
        }
    }

    /* private fun replaceFragment(fragment: Fragment) {
         activity?.supportFragmentManager?.commit {
             setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
             replace(R.id.FrameLayoutMain, fragment)
             activity?.title = getString(R.string.add_refuel_replace_fragment_title)
         }
     }*/

    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Image Uri nie będzie nullem dla RESULT_OK
                    val fileUri = data?.data!!

                    // Ustawianie podglądu zdjęcia
                    binding.addRefuelReceiptImageView.setImageURI(fileUri)
                }
                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(
                        context,
                        getString(R.string.add_car_cancel_pick_photo),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val startForProfileImageResult2 =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Image Uri nie będzie nullem dla RESULT_OK
                    val fileUri = data?.data!!

                    // Ustawianie podglądu zdjęcia
                    binding.addRefuelReceiptImageView2.setImageURI(fileUri)
                }
                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(
                        context,
                        getString(R.string.add_car_cancel_pick_photo),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val startForProfileImageResult3 =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Image Uri nie będzie nullem dla RESULT_OK
                    val fileUri = data?.data!!

                    // Ustawianie podglądu zdjęcia
                    binding.addRefuelReceiptImageView3.setImageURI(fileUri)
                }
                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(
                        context,
                        getString(R.string.add_car_cancel_pick_photo),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addRefuelToFirebase() {
        // Zmienne pobierające input użytkownika
        val fuelAmount = binding.fuelAmountInput
        val fuelPricePerUnit = binding.fuelPricePerUnitInput
        val totalCost = binding.refuelTotalCostInput
        val carMileage = binding.refuelCarMileageEdittext
        val gasStation = binding.gasStationInput
        val id = System.currentTimeMillis()
            .toString() // Generowanie losowego ID pojazdu jako aktualny czas podany w milisekundach
        val dateSelected = binding.refuelDateSelectedEdittext

        var dateSelectedInMillis: String

        // Ustawienie hintów z jednostkami
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val unitOfVolume = preferences.getString("unit_of_volume", " L")
        val unitTotalCost = preferences.getString("currency", " zł")

        binding.fuelAmountTextfield.hint =
            getString(R.string.fragment_add_refuel_fuel_amount) + " [${unitOfVolume?.trim()}]"
        binding.fuelPricePerUnitTextfield.hint =
            getString(R.string.fragment_add_refuel_fuel_price) + " [${unitTotalCost?.trim()}]"
        binding.fuelCostTextfield.hint =
            getString(R.string.fragment_add_refuel_total_cost) + " [${unitTotalCost?.trim()}]"

        // TextWatcher do przeliczania całkowitej ceny paliwa
        val refuelTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (fuelAmount.text.toString().isNotEmpty() && fuelAmount.text.toString()
                        .toFloat() > 0 && fuelPricePerUnit.text.toString().isNotEmpty()
                ) {
                    val finalCost =
                        fuelAmount.text.toString().toFloat() * fuelPricePerUnit.text.toString()
                            .toFloat()
                    //Formatowanie aby wyświetlało 2 miejsca po przecinku
                    //bez wyświetlania samotnych zer
                    val finalCostFormatted = DecimalFormat("0.##")
                    totalCost.setText(finalCostFormatted.format(finalCost).replace(',', '.'))
                }
            }
        }
        fuelAmount.addTextChangedListener(refuelTextWatcher)
        fuelPricePerUnit.addTextChangedListener(refuelTextWatcher)

        // DATEPICKER
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_picker_pick_date))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        todayDateInMillis = MaterialDatePicker.todayInUtcMilliseconds().toString()

        //Wpisanie dzisiejszej daty do pola DateSelected
        dateSelected.setText(newDate)
        dateSelectedInMillis = todayDateInMillis

        // W przypadku kliknięcia ikony kalendarza otwórz okno wybierania daty
        binding.imageViewRefuelPickDate.setOnClickListener {
            datePicker.show(parentFragmentManager, getString(R.string.date_picker_pick_date))
        }
        // W przypadku prawidłowego wybrania daty wpisz w pole datę o podanym formacie
        // Oraz zamień datę na milisekundy
        datePicker.addOnPositiveButtonClickListener {
            dateSelected.setText(datePicker.headerText)
            val simpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val mDate: Date? = simpleDateFormat.parse(datePicker.headerText)
            //Konwersja daty na milisekundy
            dateSelectedInMillis = mDate?.time.toString()
        }

        // W przypadku kliknięcia przycisku dodaj sprawdź wpisane dane i dodaj tankowanie do bazy
        binding.buttonConfirmAddRefuel.setOnClickListener {
            if (fuelAmount.text.toString().isNotEmpty() && totalCost.text.toString()
                    .isNotEmpty() && fuelPricePerUnit.text.toString().isNotEmpty()
                && carMileage.text.toString().toInt() > lastCarMileage.toInt() &&
                dateSelected.text.toString().isNotEmpty() &&
                dateSelectedInMillis.toLong() <= todayDateInMillis.toLong()
            ) {

                // Obliczenie dystansu przejechanego pomiędzy dwoma ostatnimi tankowaniami
                val distanceBetweenRefueling =
                    carMileage.text.toString().toInt().minus(lastCarMileageDifference.toInt())

                // Odczyt ID pojazdu przez ViewModel w zależnosci od pozycji Spinnera w HomeFragment
                sharedViewModel.carId.observe(viewLifecycleOwner, { carId ->
                    if (carId != null) {
                        val refuel: MutableMap<String, Any> = HashMap()
                        refuel["fuelAmount"] = fuelAmount.text.toString()
                        refuel["totalCost"] = totalCost.text.toString()
                        refuel["fuelPrice"] = fuelPricePerUnit.text.toString()
                        refuel["statisticDate"] = dateSelected.text.toString()
                        refuel["statisticDateInMillis"] = dateSelectedInMillis
                        refuel["id"] = id
                        refuel["typeOfStatistic"] = "Tankowanie"
                        refuel["carMileage"] = carMileage.text.toString()
                        refuel["gasStation"] = gasStation.text.toString()
                        refuel["distanceTravelledSinceRefueling"] =
                            distanceBetweenRefueling.toString()

                        // Dodanie wpisu do bazy danych
                        cloud.collection("users").document(uid.toString()).collection("cars")
                            .document(carId).collection("Car_statistics").document(id).set(refuel)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    activity,
                                    getString(R.string.add_refuel_successfully_added_refuel),
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Konwersja zdjęcia na bitmapę
                                val bitmap =
                                    (binding.addRefuelReceiptImageView.drawable as BitmapDrawable).bitmap
                                val bitmap2 =
                                    (binding.addRefuelReceiptImageView2.drawable as BitmapDrawable).bitmap
                                val bitmap3 =
                                    (binding.addRefuelReceiptImageView3.drawable as BitmapDrawable).bitmap

                                val baos = ByteArrayOutputStream()
                                val baos2 = ByteArrayOutputStream()
                                val baos3 = ByteArrayOutputStream()

                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                                bitmap2.compress(Bitmap.CompressFormat.PNG, 100, baos2)
                                bitmap3.compress(Bitmap.CompressFormat.PNG, 100, baos3)

                                val data = baos.toByteArray()
                                val data2 = baos2.toByteArray()
                                val data3 = baos3.toByteArray()


                                uploadStatisticPhoto(
                                    data,
                                    carId,
                                    id,
                                    "refuelImageId1",
                                    "yyyy_MM_dd_HH_mm_ss1",
                                    "RefuelPhotos"
                                )
                                uploadStatisticPhoto(
                                    data2,
                                    carId,
                                    id,
                                    "refuelImageId2",
                                    "yyyy_MM_dd_HH_mm_ss2",
                                    "RefuelPhotos"
                                )
                                uploadStatisticPhoto(
                                    data3,
                                    carId,
                                    id,
                                    "refuelImageId3",
                                    "yyyy_MM_dd_HH_mm_ss3",
                                    "RefuelPhotos"
                                )

                                Log.d("bitmapAdd1", bitmap.byteCount.toString())
                                Log.d("bitmapAdd2", bitmap2.byteCount.toString())
                                Log.d("bitmapAdd3", bitmap3.byteCount.toString())
                                Toast.makeText(activity, data.toString(), Toast.LENGTH_SHORT).show()

                                // Przekierowanie do ekrany głównego
                                replaceFragment(HomeFragment())
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    activity,
                                    getString(R.string.add_refuel_failure_message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                })
            } else if (carMileage.text.toString().isNotEmpty() && carMileage.text.toString()
                    .toInt() < lastCarMileage.toInt()
            ) {
                Toast.makeText(
                    activity,
                    getString(R.string.add_refuel_car_mileage_must_be_greater, lastCarMileage),
                    Toast.LENGTH_LONG
                ).show()
            } else if (dateSelected.text.toString()
                    .isNotEmpty() && dateSelectedInMillis.toLong() > todayDateInMillis.toLong()
            ) {
                Toast.makeText(
                    activity,
                    getString(R.string.add_refuel_date_cant_be_older_than_today),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    activity,
                    getString(R.string.add_refuel_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show() //tu sie doda wyswietlanie gwiazdki, wymaganie pól i skrócenie Toasta
            }
        }
    }
}
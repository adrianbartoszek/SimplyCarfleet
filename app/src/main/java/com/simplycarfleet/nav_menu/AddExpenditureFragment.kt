package com.simplycarfleet.nav_menu

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.simplycarfleet.R
import com.simplycarfleet.activities.MainActivity
import com.simplycarfleet.databinding.FragmentAddExpenditureBinding
import com.simplycarfleet.functions.FunctionsAndValues
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class AddExpenditureFragment : FunctionsAndValues() {
    //Zmienne binding
    private var _binding: FragmentAddExpenditureBinding? = null
    private val binding get() = _binding!!

    // Inne zmienne
    private var lastCarMileage = "0"
    private var todayDateInMillis = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddExpenditureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Usunięcie ActionBara we fragmencie
        (activity as MainActivity?)?.supportActionBar?.hide()
        // W przypadku kliknięcia przycisku anuluj przekieruj do poprzedniego fragmentu
        binding.buttonCancelAddExpenditure.setOnClickListener {
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
            cloudCarCollectionReference.get().addOnCompleteListener { result ->
                fun onComplete(task: Task<QuerySnapshot?>) {
                    if (task.isSuccessful) {
                        for (document in task.result!!) {
                            lastCarMileage = document.getString("carMileage").toString()
                        }
                        binding.expenditureCarMileageTextfield.hint =
                            getString(
                                R.string.add_expenditure_car_mileage_textfield_hint,
                                lastCarMileage
                            ) + " [${unitOfDistance?.trim()}]"
                    }
                }
                onComplete(result)
            }
        })

        // Wywołanie funkcji dodającej tankowanie
        addExpenditureToFirebase()

        // Wstawienie defaultowego zdjęcia samochodu
        Glide.with(view)
            .load("https://firebasestorage.googleapis.com/v0/b/simply-carfleet.appspot.com/o/statisticsImages%2Freceipt_default_image.png?alt=media&token=8be25f02-e89a-4cbd-96e2-bc28c57bc289")
            .into(binding.receiptImageViewAddExpenditure)
        Glide.with(view)
            .load("https://firebasestorage.googleapis.com/v0/b/simply-carfleet.appspot.com/o/statisticsImages%2Freceipt_default_image.png?alt=media&token=8be25f02-e89a-4cbd-96e2-bc28c57bc289")
            .into(binding.receiptImageView2AddExpenditure)
        Glide.with(view)
            .load("https://firebasestorage.googleapis.com/v0/b/simply-carfleet.appspot.com/o/statisticsImages%2Freceipt_default_image.png?alt=media&token=8be25f02-e89a-4cbd-96e2-bc28c57bc289")
            .into(binding.receiptImageView3AddExpenditure)

        binding.receiptImageViewAddExpenditure.setOnClickListener {
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
        binding.receiptImageView2AddExpenditure.setOnClickListener {
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
        binding.receiptImageView3AddExpenditure.setOnClickListener {
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

    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Image Uri nie będzie nullem dla RESULT_OK
                    val fileUri = data?.data!!

                    // Ustawianie podglądu zdjęcia
                    binding.receiptImageViewAddExpenditure.setImageURI(fileUri)
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
                    binding.receiptImageView2AddExpenditure.setImageURI(fileUri)
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
                    binding.receiptImageView3AddExpenditure.setImageURI(fileUri)
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

    private fun addExpenditureToFirebase() {
        // Zmienne pobierające input użytkownika
        val expenditureType = binding.expenditureTypeEdittext
        val totalCost = binding.expenditureTotalCostEdittext
        val carMileage = binding.expenditureCarMileageEdittext
        val id = System.currentTimeMillis()
            .toString() // Generowanie losowego ID pojazdu jako aktualny czas podany w milisekundach
        val dateSelected = binding.expenditureDateSelectedEdittext
        var dateSelectedInMillis: String

        // Ustawienie hintów z jednostkami
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences("Prefix", Context.MODE_PRIVATE)
        val unitTotalCost = preferences.getString("currency", " zł")

        binding.expenditureTotalCostTextfield.hint =
            getString(R.string.fragment_add_expenditure_expenditure_total_cost) + " [${unitTotalCost?.trim()}]"

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
        binding.imageViewExpenditurePickDate.setOnClickListener {
            datePicker.show(parentFragmentManager, getString(R.string.date_picker_pick_date))

        }

        // W przypadku prawidłowego wybrania daty wpisz w pole datę o podanym formacie
        // Oraz zamień datę na milisekundy
        datePicker.addOnPositiveButtonClickListener {
            dateSelected.setText(datePicker.headerText)
            //Konwersja daty na milisekundy
            val simpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val mDate: Date? = simpleDateFormat.parse(datePicker.headerText)
            dateSelectedInMillis = mDate?.time.toString()
        }
        // W przypadku kliknięcia przycisku dodaj sprawdź wpisane dane i dodaj wydatek do bazy
        binding.buttonConfirmAddExpenditure.setOnClickListener {
            if (expenditureType.text.toString().isNotEmpty() && totalCost.text.toString()
                    .isNotEmpty() && carMileage.text.toString()
                    .isNotEmpty() && dateSelected.text.toString().isNotEmpty()
                && carMileage.text.toString().toInt() > lastCarMileage.toInt() &&
                dateSelected.text.toString().isNotEmpty() &&
                dateSelectedInMillis.toLong() <= todayDateInMillis.toLong()
            ) {
                // Odczyt ID pojazdu przez ViewModel w zależnosci od pozycji Spinnera w HomeFragment
                sharedViewModel.carId.observe(viewLifecycleOwner, { carId ->
                    if (carId != null) {
                        val expenditure: MutableMap<String, Any> = HashMap()
                        expenditure["typeOfStatistic"] = "Wydatek"
                        expenditure["expenditureType"] = expenditureType.text.toString()
                        expenditure["totalCost"] = totalCost.text.toString()
                        expenditure["carMileage"] = carMileage.text.toString()
                        expenditure["statisticDate"] = dateSelected.text.toString()
                        expenditure["statisticDateInMillis"] = dateSelectedInMillis
                        expenditure["id"] = id

                        // Dodanie wpisu do bazy danych
                        cloud.collection("users").document(uid.toString()).collection("cars")
                            .document(carId).collection("Car_statistics").document(id)
                            .set(expenditure)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    activity,
                                    getString(R.string.add_expenditure_successfully_added_expenditure),
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Konwersja zdjęcia na bitmapę
                                val bitmap =
                                    (binding.receiptImageViewAddExpenditure.drawable as BitmapDrawable).bitmap
                                val bitmap2 =
                                    (binding.receiptImageView2AddExpenditure.drawable as BitmapDrawable).bitmap
                                val bitmap3 =
                                    (binding.receiptImageView3AddExpenditure.drawable as BitmapDrawable).bitmap

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
                                    "expenditureImageId1",
                                    "yyyy_MM_dd_HH_mm_ss1",
                                    "ExpenditurePhotos"
                                )
                                uploadStatisticPhoto(
                                    data2,
                                    carId,
                                    id,
                                    "expenditureImageId2",
                                    "yyyy_MM_dd_HH_mm_ss2",
                                    "ExpenditurePhotos"
                                )
                                uploadStatisticPhoto(
                                    data3,
                                    carId,
                                    id,
                                    "expenditureImageId3",
                                    "yyyy_MM_dd_HH_mm_ss3",
                                    "ExpenditurePhotos"
                                )


                                // Przekierowanie do ekrany głównego
                                replaceFragment(HomeFragment())
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    activity,
                                    getString(R.string.add_expenditure_failure_message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                })
            } else if (carMileage.text.toString().isNotEmpty() && carMileage.text.toString()
                    .toInt() - 1 < lastCarMileage.toInt()
            ) {
                Toast.makeText(
                    activity,
                    getString(R.string.add_expenditure_car_mileage_must_be_greater, lastCarMileage),
                    Toast.LENGTH_LONG
                ).show()
            } else if (dateSelected.text.toString()
                    .isNotEmpty() && dateSelectedInMillis.toLong() > todayDateInMillis.toLong()
            ) {
                Toast.makeText(
                    activity,
                    getString(R.string.add_expenditure_date_cant_be_older_than_today),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    activity,
                    getString(R.string.add_expenditure_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show() //tu sie doda wyswietlanie gwiazdki, wymaganie pól i skrócenie Toasta
            }
        }
    }
}
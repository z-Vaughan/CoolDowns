package com.example.cooldowns

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SharedViewModel : ViewModel() {
    val fragmentItems = MutableLiveData<List<FragmentItem>>(emptyList())
    val fragmentTitles = MutableLiveData<Map<String, String>>()
    // Add methods to update fragmentItems as needed
}
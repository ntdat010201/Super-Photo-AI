package com.example.superphoto.data.model

data class Language(
    val name: String,
    val code: String,
    val flagResId: Int,
    var isSelected: Boolean = false
)
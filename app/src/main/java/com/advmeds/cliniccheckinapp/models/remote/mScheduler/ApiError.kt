package com.advmeds.cliniccheckinapp.models.remote.mScheduler

import com.advmeds.cliniccheckinapp.R

enum class ApiError(val rawValue: Int) {
    PARAMS_NULL(10001),
    CLINIC_NOT_FOUND(10002),
    DOCTOR_NOT_FOUND(10003),
    DIVISION_NOT_FOUND(10004),
    PATIENT_NAME_NULL(10005),
    PATIENT_MOBILE_NULL(10006),
    PATIENT_BIRTH_NULL(10007),
    PATIENT_NATIONAL_ID_NULL(10008),
    DATETIME_WRONG(10009),
    APPOINTMENT_NOT_FOUND(10010),
    CHECK_IN_OVERTIME(10011),
    CHECK_IN_TIME_NOT_UP(10012),
    CHECK_IN_AGAIN(10013),
    SCHEDULE_NOT_FOUND(10014),
    SCHEDULE_FULLY_BOOKED(10015),
    APPOINTMENT_ALREADY_EXISTS(10016),
    DEPARTMENT_IS_CLOSED_FOR_CHECK_IN(10017);

    companion object {
        fun initWith(rawValue: Int) = values().find { it.rawValue == rawValue }
    }

    val resStringID: Int
        get() = when(this) {
            PARAMS_NULL -> R.string.mScheduler_api_error_10001
            CLINIC_NOT_FOUND -> R.string.mScheduler_api_error_10002
            DOCTOR_NOT_FOUND -> R.string.mScheduler_api_error_10003
            DIVISION_NOT_FOUND -> R.string.mScheduler_api_error_10004
            PATIENT_NAME_NULL -> R.string.mScheduler_api_error_10005
            PATIENT_MOBILE_NULL -> R.string.mScheduler_api_error_10006
            PATIENT_BIRTH_NULL -> R.string.mScheduler_api_error_10007
            PATIENT_NATIONAL_ID_NULL -> R.string.mScheduler_api_error_10008
            DATETIME_WRONG -> R.string.mScheduler_api_error_10009
            APPOINTMENT_NOT_FOUND -> R.string.mScheduler_api_error_10010
            CHECK_IN_OVERTIME -> R.string.mScheduler_api_error_10011
            CHECK_IN_TIME_NOT_UP -> R.string.mScheduler_api_error_10012
            CHECK_IN_AGAIN -> R.string.mScheduler_api_error_10013
            SCHEDULE_NOT_FOUND -> R.string.mScheduler_api_error_10014
            SCHEDULE_FULLY_BOOKED -> R.string.mScheduler_api_error_10015
            APPOINTMENT_ALREADY_EXISTS -> R.string.mScheduler_api_error_10016
            DEPARTMENT_IS_CLOSED_FOR_CHECK_IN -> R.string.mScheduler_api_error_10017
        }
}
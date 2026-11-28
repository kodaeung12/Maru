package com.project.maru.experience.location

@ConsistentCopyVisibility
data class EventCoordinates private constructor(val latitude: Double, val longitude: Double) {
    companion object {
        fun fromApi(latitude: String?, longitude: String?): EventCoordinates? {
            val lat = latitude?.trim()?.toDoubleOrNull() ?: return null
            val lng = longitude?.trim()?.toDoubleOrNull() ?: return null
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0 || (lat == 0.0 && lng == 0.0)) return null
            return EventCoordinates(if (lat == 0.0) 0.0 else lat, if (lng == 0.0) 0.0 else lng)
        }
    }
}

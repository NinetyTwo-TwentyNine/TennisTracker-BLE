package com.example.tennistracker.data

import com.example.tennistracker.data.Constants.APP_TENNIS_MAX_RADIAN
import com.example.tennistracker.data.Constants.APP_TENNIS_MAX_SPEED
import com.example.tennistracker.data.Constants.APP_TENNIS_MAX_STRENGTH

data class TennisHit(private var strength: Float, private var speed: Float, private var radian: Float) {
    init {
        if (strength < 0) {
            strength = 0F
        } else if (strength > APP_TENNIS_MAX_STRENGTH) {
            strength = APP_TENNIS_MAX_STRENGTH.toFloat()
        }

        if (speed < 0) {
            speed = 0F
        } else if (speed > APP_TENNIS_MAX_SPEED) {
            speed = APP_TENNIS_MAX_SPEED.toFloat()
        }

        if (radian < 0) {
            radian = 0F
        } else if (radian > APP_TENNIS_MAX_RADIAN) {
            radian = APP_TENNIS_MAX_RADIAN.toFloat()
        }
    }

    fun getStrength(): Float {
        return strength
    }
    fun getSpeed(): Float {
        return speed
    }
    fun getRadian(): Float {
        return radian
    }
}

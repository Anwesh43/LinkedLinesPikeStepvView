package com.anwesh.uiprojects.linkedlinepikesstepview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.linepikesstepview.LinePikesStepView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LinePikesStepView.create(this)
    }
}

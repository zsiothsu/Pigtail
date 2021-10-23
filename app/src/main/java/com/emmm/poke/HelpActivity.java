package com.emmm.poke;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;

import com.emmm.poke.databinding.HelpBinding;
import com.emmm.poke.databinding.JoinGameBinding;

public class HelpActivity extends Activity {
    HelpBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = HelpBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
    }
}

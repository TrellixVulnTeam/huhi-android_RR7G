package org.chromium.chrome.browser.onboarding;


import android.app.Fragment;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;

import org.chromium.chrome.browser.onboarding.OnViewPagerAction;
import org.chromium.chrome.browser.HuhiRewardsHelper;

import org.chromium.chrome.R;

public class HuhiShieldsOnboardingFragment extends Fragment {

    private TextView tvText;

    private Button btnSkip, btnNext;

    private OnViewPagerAction onViewPagerAction;

    public HuhiShieldsOnboardingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_huhi_shields_onboarding, container, false);

        initializeViews(root);

        setActions();

        return root;
    }

    public void setOnViewPagerAction(OnViewPagerAction onViewPagerAction) {
        this.onViewPagerAction = onViewPagerAction;
    }

    private void initializeViews(View root) {
        tvText = root.findViewById(R.id.section_text);

        btnSkip = root.findViewById(R.id.btn_skip);
        btnNext = root.findViewById(R.id.btn_next);
    }


    private void setActions() {
        String huhiShieldsText = "<b>"+getResources().getString(R.string.block)+"</b> "+getResources().getString(R.string.huhi_shields_onboarding_text);
        Spanned textToInsert = HuhiRewardsHelper.spannedFromHtmlString(huhiShieldsText);
        tvText.setText(textToInsert);
        tvText.setMovementMethod(new ScrollingMovementMethod());

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert onViewPagerAction != null;
                if (onViewPagerAction != null)
                    onViewPagerAction.onSkip();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert onViewPagerAction != null;
                if (onViewPagerAction != null)
                    onViewPagerAction.onNext();
            }
        });
    }
}

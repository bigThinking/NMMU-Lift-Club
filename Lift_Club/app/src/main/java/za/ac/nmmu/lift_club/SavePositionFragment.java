package za.ac.nmmu.lift_club;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import za.ac.nmmu.lift_club.util.Activity;
import za.ac.nmmu.lift_club.util.Position;

/**
 * Created by s210036575 on 2015-07-07.
 */
public class SavePositionFragment extends Fragment{

    TextView txtPoint;
    Button btnSave;
    Activity a;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.save_curr_position, container, false);

        txtPoint = (TextView)layout.findViewById(R.id.txtPoint);
        txtPoint.setText("Obtaining GPS point");
        btnSave = (Button)layout.findViewById(R.id.btnSave1);
        btnSave.setEnabled(false);
        final EditText editName =  (EditText)layout.findViewById(R.id.edtPointName);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString();

                if(name.trim().compareTo("") != 0) {
                    //check points to make sure no point with same name
                    for (Position p : ((MainActivity) SavePositionFragment.this.getActivity()).points) {
                        if (p.name.compareTo(name) == 0) {
                            showErrorDialog("Invalid entry", "A position with the given name already exist");
                            return;
                        }
                    }

                    a.pointName = name;
                    ((MainActivity) getActivity()).saveCurrentPosition(a);
                }else showErrorDialog("Invalid entry", "Please enter a name for the position");
            }
        });

        return layout;
    }

    public void showErrorDialog(String title, String message)
    {
        ErrorDialogFragment edf = new ErrorDialogFragment();
        edf.title = title;
        edf.errorMessage = message;
        edf.show(getActivity().getSupportFragmentManager(), "");
    }

    public void update(Activity a)
    {
        this.a = a;
        txtPoint.setText("Current point: " + a.point.toString());
        btnSave.setEnabled(true);
    }

}

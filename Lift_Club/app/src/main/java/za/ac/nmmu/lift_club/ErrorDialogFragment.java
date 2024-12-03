
package za.ac.nmmu.lift_club;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by s210036575 on 2015-09-01.
 */

public class ErrorDialogFragment extends DialogFragment {

    public String errorMessage, title;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),R.style.AlertDialogCustom);
        builder.setTitle(title).setMessage(errorMessage).setPositiveButton("Ok",null);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
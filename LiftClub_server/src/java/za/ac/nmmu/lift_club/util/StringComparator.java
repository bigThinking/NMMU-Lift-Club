/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.ac.nmmu.lift_club.util;

import java.util.Comparator;

/**
 *
 * @author Joshua
 */
public class StringComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        return ((String)o1).compareTo((String)o2);
    }
    
}

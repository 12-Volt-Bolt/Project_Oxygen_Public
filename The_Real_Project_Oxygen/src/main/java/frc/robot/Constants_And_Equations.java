/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

/**
 * Add your docs here.
 */
public class Constants_And_Equations {

    public static double deadzone(double d, double deadzone){
        return Math.abs(d) > Math.abs(deadzone) ? d : 0;
    }

    public static double parabola(double d){
        double dten = d*10;
        if (dten > 0) {
            return Math.pow(dten, 2)/10;
        } else {
            return (Math.pow(dten, 2)/10)*-1;
        }
    }



}

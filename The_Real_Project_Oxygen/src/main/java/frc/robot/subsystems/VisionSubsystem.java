/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.subsystems;

import javax.swing.text.StyleContext.SmallAttributeSet;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSink;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.GenericHID.Hand;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants_And_Equations;
import frc.robot.OI;
import frc.robot.Robot;
import frc.robot.RobotMap;
import frc.robot.VisionMath;
import edu.wpi.cscore.VideoSource;

/**
 * This class holds all methods and objects related to the camera We use both
 * the Logitech 930e and the Microsoft LifeCam 3000
 */

public class VisionSubsystem extends Subsystem {

  // USB Camera Objects
  public static UsbCamera topCam, bottomCam;

  // Camera Servers
  public static CameraServer camServerTop, camServerBottom;

  // VideoSink Server
  public static VideoSink theOnlyCamServer;

  // M-JPEG Servers
  public static MjpegServer mjpegServerTop, mjpegServerBottom;

  // Camera names
  public static final String TOP_CAM_NAME = "Top Camera";
  public static final String BOTTOM_CAM_NAME = "Bottom Camera";

  // Camera frame rate
  public static final int TOP_CAM_FPS = 5;
  public static final int BOTTOM_CAM_FPS = 5;

  // Total number of pixel columns
  public static final int TOP_CAM_COL_PIXEL_NUM = 480;
  public static final int BOTTOM_CAM_COL_PIXEL_NUM = 240;

  // Total number of pixel rows
  public static final int TOP_CAM_ROW_PIXEL_NUM = 640;
  public static final int BOTTOM_CAM_ROW_PIXEL_NUM = 320;

  // Smartdashboard variables
  public static int measCenterXPixels;
  public static int measSeparationPixels;
  public static int measAlAngleDegrees;
  public static int measAlCenterXPixels;
  public static boolean isProcessCmdBool;

  // Smartdashboard Network Table Strings
  public static final String measCenterXString = "DB/Slider 0";
  public static final String measSeparationString = "DB/Slider 1";
  public static final String measAlCenterXString = "DB/Slider 2";
  public static final String measAlAngleDegreesString = "DB/Slider 3";
  public static final String isProcessCMDString = "DB/Button 0";

  // Data obtained from LabVIEW vision program
  public static double targetCenterXInPixels;
  public static double vtSeparationInPixels;
  public static double alCenterXinPixels;
  public static double alAngleInDegrees;

  // Calibration Data:
  public static final int caliVTDistanceCm = 1;
  public static final int caliVTCenterInPixels = 1;
  public static final int caliVTSeparationInPixels = 1;
  public static final int caliAlCenterInPixels = 1;
  public static final int caliAlAngleInDegrees = 1;

  // PID Controllers
  public PIDController rotationController;
  public PIDController strafeController;
  public PIDController verticalController;

  // PID Constants
  private final int RotationP = 0;
  private final int RotationI = 0;
  private final int RotationD = 0;
  private final int RotationF = 0;

  // PID Constants
  private final int StrafeP = 0;
  private final int StrafeI = 0;
  private final int StrafeD = 0;
  private final int StrafeF = 0;

  // PID Constants
  private final int verticalP = 0;
  private final int verticalI = 0;
  private final int verticalD = 0;
  private final int verticalF = 0;

  // Constants
  private static final int NO_DATA = -888;

  public VisionSubsystem() {
    topCam = CameraServer.getInstance().startAutomaticCapture(TOP_CAM_NAME, RobotMap.CAMERA_ZERO_ID);
    topCam.setResolution(TOP_CAM_ROW_PIXEL_NUM, TOP_CAM_COL_PIXEL_NUM);
    topCam.setFPS(TOP_CAM_FPS);

    bottomCam = CameraServer.getInstance().startAutomaticCapture(BOTTOM_CAM_NAME, RobotMap.CAMERA_ONE_ID);
    bottomCam.setResolution(BOTTOM_CAM_ROW_PIXEL_NUM, BOTTOM_CAM_COL_PIXEL_NUM);
    bottomCam.setFPS(BOTTOM_CAM_FPS);

    bottomCam.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);
    topCam.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    targetCenterXInPixels = NO_DATA;
    vtSeparationInPixels = NO_DATA;
    alCenterXinPixels = NO_DATA;
    alAngleInDegrees = NO_DATA;

    // rotationController = new PIDController(Kp, Ki, Kd, source, output)
    // rotationController = new PIDController(Kp, Ki, Kd, source, output);
    // strafeController = new PIDController(Kp, Ki, Kd, source, output);
    // verticalController = new PIDController(Kp, Ki, Kd, source, output);

  }

  @Override
  public void initDefaultCommand() {

  }

  public int distanceFromCamToTargetInCM() {
    return ((caliVTSeparationInPixels) / (measSeparationPixels)) * (caliVTDistanceCm);
  }

  public int lateralOffsetToTargetInCM() {
    return (int) ((int) (caliVTCenterInPixels / measSeparationPixels) * (20.32 / vtSeparationInPixels));
  }

  // If positive, strafe right
  // If negitive, strafe left
  public int aLineCamOffset() {
    return (int) (((caliAlCenterInPixels) - measCenterXPixels) * (20.32 / vtSeparationInPixels));
  }
  
  // If positive, rotate Left
  // If negitive, rotate Right
  public int aLineAngleOffset() {
    return (measAlAngleDegrees - caliAlAngleInDegrees);
  }

  public void updateVisionObject() {
    if (OI.visionStartCombo()) {
      SmartDashboard.putBoolean(isProcessCMDString, true);
    } else {
      SmartDashboard.putBoolean(isProcessCMDString, false);

    }

    measCenterXPixels = (int) SmartDashboard.getNumber(measCenterXString, -888);
    measSeparationPixels = (int) SmartDashboard.getNumber(measSeparationString, -888);
    measAlAngleDegrees = (int) SmartDashboard.getNumber(measAlAngleDegreesString, -888);
    measAlCenterXPixels = (int) SmartDashboard.getNumber(measAlCenterXString, -888);
  }

  public void runRotationController(double var) {
    float headingError = 0f;
    float rotationAdjust = 0.0f;

    if (var > 0) { 
      
    }

    else if (var < 0) {

    }

    else {
      // stop please
    }

  }

  public void runStrafeController(double vari) {
    float headingError = 0f;
    float strafeAdjust = 0.0f;

    double scaledVari = vari / 100;

    if (vari > 0) {
    
    }

    else if (vari < 0) {

    }

    else {
      // stop please
    }

  }

  public void runVerticalController(double var) {
    float headingError = 0f;
    float verticalAdjust = 0.0f;

    if (var > 0) {
    
    }

    else if (var < 0) {

    }

    else {
      // stop please
    }

  }

}

// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.List;

import javax.lang.model.element.Parameterizable;

//import java.util.List;

import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
//import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.path.PathPlannerPath;
//import com.pathplanner.lib.path.PathPlannerPath;
import com.ctre.phoenix6.mechanisms.swerve.SwerveModule.DriveRequestType;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.ParallelRaceGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
//import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.StartEndCommand;
import edu.wpi.first.wpilibj2.command.SwerveControllerCommand;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
//import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import frc.robot.subsystems.drivetrain.generated.TunerConstants;
import frc.robot.util.LimelightHelpers;
//import frc.robot.util.controllerUtils.MultiButton;
//import frc.robot.subsystems.ClimberSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.VisionSubsystem;
import frc.robot.subsystems.drivetrain.*;



public class RobotContainer {
    public final ShooterSubsystem shooterSubsystem = new ShooterSubsystem();
//    public final ClimberSubsystem climberSubsystem = new ClimberSubsystem();
    public final IntakeSubsystem intakeSubsystem = new IntakeSubsystem();
    public final VisionSubsystem visionSubsystem = new VisionSubsystem();

  private double MaxSpeed = 5 *(.75); // 6 meters per second desired top speed change the decimal for speeding up
  private double MaxAngularRate = 2 * Math.PI *(.9); // 3/4 of a rotation per second max angular velocity

  /* Setting up bindings for necessary control of the swerve drive platform */
  private final CommandXboxController driveStick = new CommandXboxController(0); //drivestick
  private final CommandXboxController opStick = new CommandXboxController(1); // My joystick

  public final CommandSwerveDrivetrain drivetrain = TunerConstants.DriveTrain; // My drivetrain

  private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
      .withDeadband(MaxSpeed * 0.025).withRotationalDeadband(MaxAngularRate * 0.025) // Add a 10% deadband
      .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // I want field-centric
                                                               // driving in open loop
  private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
  private final SwerveRequest.PointWheelsAt point = new SwerveRequest.PointWheelsAt();
  private final Telemetry logger = new Telemetry(MaxSpeed);

  SendableChooser<Command> m_chooser = new SendableChooser<>();
  private final Command Top2Piece = new PathPlannerAuto("Top2Piece");
  private final Command Top3Close = new PathPlannerAuto("Top3Close");
  private final Command Top3TopMid = new PathPlannerAuto("Top3TopMid");
  private final Command Top4Piece2CloseTopMid = new PathPlannerAuto("Top4Piece2CloseTopMid");
  private final Command Top4PieceCloseAll = new PathPlannerAuto("Top4PieceCloseAll");
  private final Command Top5Piece2Close2TopMid = new PathPlannerAuto("Top5Piece2Close2TopMid");

  private final Command Bottom2Piece = new PathPlannerAuto("Bottom2Piece");
  private final Command Bottom3Close = new PathPlannerAuto("Bottom3Close");
  private final Command Bottom3Mid = new PathPlannerAuto("Bottom3Mid");
  private final Command Bottom4Close2Mid = new PathPlannerAuto("Bottom4Close2Mid");
  private final Command Bottom4CloseAll = new PathPlannerAuto("Bottom4CloseAll");
  
  private final Command Mid2 = new PathPlannerAuto("Mid2");
  private final Command Mid3CenterMid = new PathPlannerAuto("Mid3CenterMid");
  private final Command Mid4CenterMidBelow = new PathPlannerAuto("Mid4CenterMidBelow");
  private final Command Mid4CenterMidUp = new PathPlannerAuto("Mid4CenterMidUp");
  private final Command Mid5CenterMid3 = new PathPlannerAuto("Mid5CenterMid3");


  // simple proportional turning control with Limelight.
  // "proportional control" is a control algorithm in which the output is proportional to the error.
  // in this case, we are going to return an angular velocity that is proportional to the 
  // "tx" value from the Limelight.
  double limelight_aim_proportional()
  {    
    // kP (constant of proportionality)
    // this is a hand-tuned number that determines the aggressiveness of our proportional control loop
    // if it is too high, the robot will oscillate.
    // if it is too low, the robot will never reach its target
    // if the robot never turns in the correct direction, kP should be inverted.
    double kP = .035;

    // tx ranges from (-hfov/2) to (hfov/2) in degrees. If your target is on the rightmost edge of 
    // your limelight 3 feed, tx should return roughly 31 degrees.
    double targetingAngularVelocity = LimelightHelpers.getTX("limelight") * kP;

    // convert to radians per second for our drive method
    targetingAngularVelocity *= MaxAngularRate;

    //invert since tx is positive when the target is to the right of the crosshair
    targetingAngularVelocity *= -1.0;

    return targetingAngularVelocity;
  }

  // simple proportional ranging control with Limelight's "ty" value
  // this works best if your Limelight's mount height and target mount height are different.
  // if your limelight and target are mounted at the same or similar heights, use "ta" (area) for target ranging rather than "ty"
  double limelight_range_proportional()
  {    
    double kP = .1;
    double targetingForwardSpeed = LimelightHelpers.getTY("limelight") * kP;
    targetingForwardSpeed *= MaxSpeed;
    targetingForwardSpeed *= -1.0;
    return targetingForwardSpeed;
  }




  private void configureBindings() {

    //Driver Xbox Controller
    drivetrain.setDefaultCommand( // Drivetrain will execute this command periodically
        drivetrain.applyRequest(() -> drive.withVelocityX(-driveStick.getLeftY() * MaxSpeed) // Drive forward with
                                                                                           // negative Y (forward)
            .withVelocityY(-driveStick.getLeftX() * MaxSpeed) // Drive left with negative X (left)
            .withRotationalRate(-driveStick.getRightX() * MaxAngularRate) // Drive counterclockwise with negative X (left)
        ));

    driveStick.leftTrigger().whileTrue( // Drivetrain will execute this command periodically
        drivetrain.applyRequest(() -> drive.withVelocityX(-driveStick.getLeftY() * MaxSpeed*(.4)) // Drive forward with
                                                                                           // negative Y (forward)
            .withVelocityY(-driveStick.getLeftX() * MaxSpeed*(.4)) // Drive left with negative X (left)
            .withRotationalRate(-driveStick.getRightX() * MaxAngularRate*(.4) // Drive counterclockwise with negative X (left)
        )));    
       
  
    driveStick.a().whileTrue(drivetrain.applyRequest(() -> brake));
    driveStick.b().whileTrue(drivetrain
        .applyRequest(() -> point.withModuleDirection(new Rotation2d(-driveStick.getLeftY(), -driveStick.getLeftX()))));



    driveStick.leftBumper().whileTrue(

    drivetrain.applyRequest(() -> drive.withVelocityX(limelight_range_proportional())
            .withVelocityY(-driveStick.getLeftX() * MaxSpeed*(.4))
            .withRotationalRate(limelight_aim_proportional())));

            
    // reset the field-centric heading on left bumper press
   // driveStick.leftBumper().onTrue(drivetrain.runOnce(() -> drivetrain.seedFieldRelative()));

    if (Utils.isSimulation()) {
      drivetrain.seedFieldRelative(new Pose2d(new Translation2d(), Rotation2d.fromDegrees(90)));
    }
    drivetrain.registerTelemetry(logger::telemeterize);

    //Op Xbox Controller
    
    opStick.x().whileTrue(
    new StartEndCommand(() -> shooterSubsystem.shootFlywheel(.33), shooterSubsystem::stopFlywheel));

    opStick.leftTrigger().whileTrue(
    new StartEndCommand(() -> shooterSubsystem.shootFlywheel(.14), shooterSubsystem::stopFlywheel));

    opStick.b().whileTrue(
       new StartEndCommand(() -> shooterSubsystem.spinBump(-.2), shooterSubsystem::stopBump));
      
    opStick.a().whileTrue(
       new StartEndCommand(() -> shooterSubsystem.spinBump(.2), shooterSubsystem::stopBump));
   
    driveStick.rightTrigger().whileTrue(
      new StartEndCommand(()-> intakeSubsystem.roll(1), intakeSubsystem::rollStop));

    driveStick.rightTrigger().whileTrue(
      new StartEndCommand(() -> shooterSubsystem.spinBump(.4), shooterSubsystem::stopBump));

    opStick.y().whileTrue(
       new StartEndCommand(() -> shooterSubsystem.spinBump(-.5), shooterSubsystem::stopBump));

    opStick.y().whileTrue(
       new StartEndCommand(() -> intakeSubsystem.roll(-.5), intakeSubsystem::rollStop));

    
     
  //  opStick.rightTrigger().whileTrue( 
  //    new StartEndCommand(() -> climberSubsystem.climbUp(.2), climberSubsystem::stopClimb));
  //   opStick.leftTrigger().whileTrue( 
  //    new StartEndCommand(() -> climberSubsystem.climbDown(.2), climberSubsystem::stopClimb));

  }

  public RobotContainer() {
    configureBindings();
    
    m_chooser.setDefaultOption("Top2Piece", Top2Piece);
    m_chooser.addOption("Top3Close", Top3Close);
    m_chooser.addOption("Top3TopMid", Top3TopMid);
    m_chooser.addOption("Top4PieceCloseAll", Top4PieceCloseAll);
    m_chooser.addOption("Top4Piece2CloseTopMid", Top4Piece2CloseTopMid);
    m_chooser.addOption("Top5Piece2Close2TopMid", Top5Piece2Close2TopMid);

    m_chooser.addOption("Bottom2Piece", Bottom2Piece);
    m_chooser.addOption("Bottom3Close", Bottom3Close);
    m_chooser.addOption("Bottom3Mid", Bottom3Mid);
    m_chooser.addOption("Bottom4Close2Mid", Bottom4Close2Mid);
    m_chooser.addOption("Bottom4CloseAll", Bottom4CloseAll);

    m_chooser.addOption("Mid2", Mid2);
    m_chooser.addOption("Mid3CenterMid", Mid3CenterMid);
    m_chooser.addOption("Mid4CenterMidBelow", Mid4CenterMidBelow);
    m_chooser.addOption("Mid4CenterMidUp", Mid4CenterMidUp);
    m_chooser.addOption("Mid5CenterMid3", Mid5CenterMid3);



    SmartDashboard.putData(m_chooser);

    

  }

    public Command getAutonomousCommand() {
      

      return m_chooser.getSelected();
      
/* 

      return new SequentialCommandGroup(
           new ParallelCommandGroup(
              new PathPlannerAuto("Blue Shoot First"),
              new InstantCommand(()-> shooterSubsystem.shootFlywheel(.29))),
           new WaitCommand(1.5),
           new InstantCommand(()-> shooterSubsystem.spinBump(.4)),
           new WaitCommand(.5),
           new InstantCommand(()-> shooterSubsystem.stopFlywheel()),
           new InstantCommand(()-> shooterSubsystem.stopBump()),
           new InstantCommand(()-> intakeSubsystem.roll(1)),
           new InstantCommand(()-> shooterSubsystem.spinBump(.2)),
           new PathPlannerAuto("Speaker to Top Close Blue"),
           new InstantCommand(()-> shooterSubsystem.stopBump()),
           new InstantCommand(()-> shooterSubsystem.spinBump(-.2)),
           new WaitCommand(.05),
           new ParallelCommandGroup(
              new InstantCommand(()-> shooterSubsystem.shootFlywheel(.3)),
              new PathPlannerAuto("Shoot 2 Blue")),
          new WaitCommand(1.2),
          new InstantCommand(()-> shooterSubsystem.spinBump(.4)),
          new WaitCommand(1),
          new InstantCommand(()-> shooterSubsystem.stopFlywheel()),
          new InstantCommand(()-> shooterSubsystem.stopBump()),
          new InstantCommand(()-> intakeSubsystem.rollStop())
          );

      */

     // return new PathPlannerAuto("Top Blue");
}
}
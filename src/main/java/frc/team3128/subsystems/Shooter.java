package frc.team3128.subsystems;

import common.core.controllers.Controller;
import common.core.controllers.ControllerBase;
import common.core.controllers.Controller.Type;
import common.core.subsystems.NAR_PIDSubsystem;
import common.hardware.motorcontroller.NAR_CANSpark;
import common.hardware.motorcontroller.NAR_CANSpark.ControllerType;
import common.hardware.motorcontroller.NAR_Motor.Neutral;
import common.utility.narwhaldashboard.NarwhalDashboard.State;
import common.utility.shuffleboard.NAR_Shuffleboard;
import common.utility.tester.CurrentTest;
import common.utility.tester.Tester;
import common.utility.tester.Tester.UnitTest;
import edu.wpi.first.wpilibj2.command.Command;
import java.util.function.DoubleSupplier;

import static frc.team3128.Constants.ShooterConstants.*;

public class Shooter extends NAR_PIDSubsystem {

    public NAR_CANSpark leftMotor;
    public NAR_CANSpark rightMotor;

    private static Shooter instance;

    private DoubleSupplier rpmDiff;

    private DoubleSupplier kF_Func;

    private Controller rightController = new Controller(PIDConstants, Type.VELOCITY);

    private Shooter(){
        super(new Controller(PIDConstants, Type.VELOCITY));
        setConstraints(MIN_RPM, MAX_RPM);
        setTolerance(TOLERANCE);
        configMotors();
        initShuffleboard();
        rpmDiff = NAR_Shuffleboard.debug("rmpDiff", "rpmDiff", 750, 0, 0);
        NAR_Shuffleboard.addData("rmpDiff", "rightVelocity", ()-> rightMotor.getVelocity(), 1, 0);

        ControllerBase controller = getController();
        rightController.setkS(()-> controller.getkS());
        rightController.setkV(()-> controller.getkV());
        rightController.setkG(()-> controller.getkG());
        NAR_Shuffleboard.addSendable(getName(), "rightController", controller, 4, 0);
        kF_Func = NAR_Shuffleboard.debug(getName(), "kF", kF, 4, 2);
    }

    public static synchronized Shooter getInstance(){
        if(instance == null){
            instance = new Shooter();
        }
        return instance;
    }

    @Override
    public boolean atSetpoint() {
        return super.atSetpoint() && Math.abs(Math.abs(rightMotor.getVelocity()) - (getSetpoint() - rpmDiff.getAsDouble())) < TOLERANCE;
    }

    private void configMotors(){
        leftMotor = new NAR_CANSpark(LEFT_MOTOR_ID, ControllerType.CAN_SPARK_FLEX);
        rightMotor = new NAR_CANSpark(RIGHT_MOTOR_ID, ControllerType.CAN_SPARK_FLEX);
        
        leftMotor.setInverted(true);
        rightMotor.setInverted(false);
        leftMotor.setUnitConversionFactor(GEAR_RATIO);
        rightMotor.setUnitConversionFactor(GEAR_RATIO);

        leftMotor.setNeutralMode(Neutral.COAST);
        rightMotor.setNeutralMode(Neutral.COAST);
    }

    private void setPower(double power){
        disable();
        leftMotor.set(power);
        rightMotor.set(power);
    }

    @Override
    protected void useOutput(double output, double setpoint) {
        leftMotor.setVolts(setpoint != 0 ? output + kF_Func.getAsDouble() : 0);
        final double rightOutput = rightController.calculate(Math.abs(rightMotor.getVelocity()), setpoint - rpmDiff.getAsDouble());
        // rightMotor.setVolts(output);
        rightMotor.setVolts(setpoint != 0 ? rightOutput + kF_Func.getAsDouble() : 0);
    }

    @Override
    public double getMeasurement() {
        return leftMotor.getVelocity();
    }

    public Command shoot(double setpoint){
        return runOnce(() -> startPID(setpoint));
    }

    public Command setShooter(double power) {
        return runOnce(()-> setPower(power));
    }

    public Command runBottomRollers(double power) {
        return runOnce(()-> rightMotor.set(power));
    }

    public State getRunningState() {
        if (rightMotor.getState() != State.DISCONNECTED && leftMotor.getState() != State.DISCONNECTED) {
            return State.RUNNING; 
        }
        return State.DISCONNECTED;
    }

    public CurrentTest getLeftMotorTest() {
        return new CurrentTest
        (
            "testLeftMotor", 
            leftMotor, 
            CURRENT_TEST_POWER, 
            CURRENT_TEST_TIMEOUT,
            CURRENT_TEST_PLATEAU,
            CURRENT_TEST_EXPECTED_CURRENT,
            CURRENT_TEST_TOLERANCE, 
            this
        );
    }

    public CurrentTest getRightMotorTest() {
        return new CurrentTest
        (
            "testRightMotor", 
            rightMotor, 
            CURRENT_TEST_POWER, 
            CURRENT_TEST_TIMEOUT,
            CURRENT_TEST_PLATEAU,
            CURRENT_TEST_EXPECTED_CURRENT,
            CURRENT_TEST_TOLERANCE, 
            this
        );
    }

    public UnitTest getShooterTest() {
        return new SetpointTest
        (
            "testShooter",
            MAX_RPM,
            SHOOTER_TEST_PLATEAU,
            SHOOTER_TEST_TIMEOUT
        );
    }

    public void addShooterTests() {
        // Tester.getInstance().addTest("Shooter", getLeftMotorTest());
        // Tester.getInstance().addTest("Shooter", getRightMotorTest());
        Tester.getInstance().addTest("Shooter", getShooterTest());
    }
}
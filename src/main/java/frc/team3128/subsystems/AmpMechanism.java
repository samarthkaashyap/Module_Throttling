package frc.team3128.subsystems;

import common.core.controllers.TrapController;
import common.core.subsystems.PivotTemplate;
import common.hardware.motorcontroller.NAR_Motor.Neutral;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;

import static edu.wpi.first.wpilibj2.command.Commands.*;
import static frc.team3128.Constants.AmpWristConstants.*;
import static frc.team3128.Constants.IntakeConstants.CURRENT_LIMIT;

public class AmpMechanism extends PivotTemplate {

    public enum Setpoint {
        AMP(180),
        RETRACTED(0);

        private double angle;
        private Setpoint(double angle) {
            this.angle = angle;
        }
    }

    private static AmpMechanism instance;

    public static synchronized AmpMechanism getInstance() {
        if (instance == null) {
            instance = new AmpMechanism();
        }
        return instance;
    }

    private AmpMechanism() {
        super(new TrapController(PIDConstants, TRAP_CONSTRAINTS), WRIST_MOTOR);
        setkG_Function(()-> Math.cos(Units.degreesToRadians(getSetpoint())));
        setTolerance(POSITION_TOLERANCE);
    }

    @Override
    protected void configMotors() {
        WRIST_MOTOR.setUnitConversionFactor(GEAR_RATIO * 360);
        WRIST_MOTOR.setCurrentLimit(CURRENT_LIMIT);
        WRIST_MOTOR.setNeutralMode(Neutral.BRAKE);

        ROLLER_MOTOR.setNeutralMode(Neutral.COAST);
    }

    public Command pivotTo(Setpoint setpoint) {
        return pivotTo(setpoint.angle);
    }

    public Command retract() {
        return sequence(
            pivotTo(Setpoint.RETRACTED),
            runOnce(()-> ROLLER_MOTOR.set(0))
        );
    }

    public Command extend() {
        return sequence(
            pivotTo(Setpoint.AMP),
            runOnce(()-> ROLLER_MOTOR.set(AMP_POWER))
        );
    }
    
}
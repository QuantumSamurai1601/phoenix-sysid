package frc.robot.subsystems;

import static edu.wpi.first.units.Units.Volts;

import java.util.function.DoubleSupplier;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.SignalLogger;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;

import edu.wpi.first.units.Measure;
import edu.wpi.first.units.Voltage;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import frc.robot.Constants;

public class Mechanism extends SubsystemBase {
    private final TalonFX m_motorToTest = new TalonFX(Constants.LEADER_TALON_FX_ID, Constants.CANBUS);
    private final TalonFX m_follower = new TalonFX(Constants.FOLLOWER_TALON_FX_ID, Constants.CANBUS);
    private final DutyCycleOut m_joystickControl = new DutyCycleOut(0);
    private final VoltageOut m_sysidControl = new VoltageOut(0);
    private final Follower m_followerControl = new Follower(Constants.LEADER_TALON_FX_ID, true);

    private SysIdRoutine m_SysIdRoutine =
        new SysIdRoutine(
            new SysIdRoutine.Config(
                null,         // Default ramp rate is acceptable
                Volts.of(4), // Reduce dynamic voltage to 4 to prevent motor brownout
                null,          // Default timeout is acceptable
                                       // Log state with Phoenix SignalLogger class
                (state)->SignalLogger.writeString("state", state.toString())),
            new SysIdRoutine.Mechanism(
                (Measure<Voltage> volts)-> m_motorToTest.setControl(m_sysidControl.withOutput(volts.in(Volts))),
                null,
                this));

    public Mechanism() {
        setName("Intake");

        m_follower.setControl(m_followerControl);
        TalonFXConfiguration cfg = new TalonFXConfiguration();
        cfg.SoftwareLimitSwitch.ForwardSoftLimitThreshold = 0;
        cfg.SoftwareLimitSwitch.ForwardSoftLimitEnable = false;
        cfg.SoftwareLimitSwitch.ReverseSoftLimitThreshold = 0.34;
        cfg.SoftwareLimitSwitch.ReverseSoftLimitEnable = false;
        cfg.Feedback.SensorToMechanismRatio = 25;
        cfg.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
        m_motorToTest.setPosition(0.34);
        m_motorToTest.getConfigurator().apply(cfg);

        /* Speed up signals for better charaterization data */
        BaseStatusSignal.setUpdateFrequencyForAll(250,
            m_motorToTest.getPosition(),
            m_motorToTest.getVelocity(),
            m_motorToTest.getMotorVoltage());

        /* Optimize out the other signals, since they're not particularly helpful for us */
        m_motorToTest.optimizeBusUtilization();

        SignalLogger.setPath("/home/lvuser/logs/");
        SignalLogger.start();
    }

    public Command joystickDriveCommand(DoubleSupplier output) {
        return run(()->m_motorToTest.setControl(m_joystickControl.withOutput(output.getAsDouble())));
    }
    public Command sysIdQuasistatic(SysIdRoutine.Direction direction) {
        return m_SysIdRoutine.quasistatic(direction);
    }
    public Command sysIdDynamic(SysIdRoutine.Direction direction) {
        return m_SysIdRoutine.dynamic(direction);
    }
}

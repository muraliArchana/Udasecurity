package com.udacity.catpoint.security;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


/**
 Some logic and code attributed to Tony - Session Lead - Udacity, Michael - Session Lead - Udacity

 */


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private final String random= UUID.randomUUID().toString();

    @Mock
    private StatusListener statusListener;

    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private Sensor sensor;

    boolean isCatImage;
    @Mock
    private ImageService imageService;

     private SecurityService securityService;

    @BeforeEach
    void init()
    {
        securityService=new SecurityService(securityRepository,imageService);
        sensor=new Sensor(random,SensorType.DOOR);
    }

    //Unit Test 1 -If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    void IfSystemIsArmed_SensorActivated_ChangeStatusPending()
    {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }

    //Unit Test2
    //If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm on.
  @ParameterizedTest
  @EnumSource(value=ArmingStatus.class,names = {"ARMED_HOME","ARMED_AWAY"})
    void IfSystemArmed_SensorActivated_StatusPendingAlarm_ChangeStatusAlarm(ArmingStatus armingStatus)
    {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //If pending alarm and all sensors are inactive, return to no alarm state
    //Unit Test3
    @Test
    void ifStatusIsPendingAlarm_SensorsInactive_ChangeStatus_NoAlaram()
    {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor,false);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    //unit Test4 - if alarm is active then change in status should not change alarm status

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void IfAlarmStatusActive_ChangeSensorStatus_NoChangeInAlarmStatus(boolean status){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor,status);
        verify(securityRepository,never()).setAlarmStatus(any(AlarmStatus.class));

    }

    //Unit Test 5 . One sensor is active and alarm is in pending status then another sensor activate and alarm status change to Alaram.

    @Test
    void ifSensorActive_WhileAlreadyActive_AlarmIsPendingStatus_ChangeToAlarmState()
    {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor2=new Sensor("Front Window", SensorType.WINDOW);
        securityService.changeSensorActivationStatus(sensor2,true);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }

    //Unit Test 6 if a sensor is deactivated while already inactive,then no change in alarm status.
   @Test
    void IfSensorDeactivate_WhileAlreadyInactive_NoChangeToAlarmState()
    {
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor,false);
        verify(securityRepository,never()).setAlarmStatus(any(AlarmStatus.class));

    }

    //Unit Test7 - When Cat image in camera while system is Armed then change AlarmStatus into Alarm.
@Test
void ifCatImage_SystemArmedHome_ChangeStatusIntoAlarm()
{
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    BufferedImage cat=new BufferedImage(4,4,4);
    when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
    securityService.processImage(cat);
    verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);

}

//Unit Test 8 If No sensors active and Image contains no Cat then change alarm status into NoAlarm.
@Test
void ifNoCatImage_NoSensorsActive_ChangeStatusIntoNoAlarm()
{
    BufferedImage cat=new BufferedImage(4,4,4);
    when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);
    securityService.processImage(cat);
    verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
}

//Unit Test 9 If system disarmed change status to NoAlarm
@Test
void ifSystemDisarmed_ChangeStatusToNoAlarm()
{
    ArmingStatus status=ArmingStatus.DISARMED;
    securityService.setArmingStatus(status);
    verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(securityRepository,times(1)).setArmingStatus(ArmingStatus.DISARMED);

}

//Unit test 10. If system is Armed set All sensors to inactive.

@ParameterizedTest
@EnumSource(value=ArmingStatus.class,names = {"ARMED_HOME","ARMED_AWAY"})
void IfSystemIsArmed_SetAllSensors_Inactive(ArmingStatus armingStatus)
{

    Set<Sensor> sensors=new HashSet<>();
    Sensor newSensor1 =new Sensor(random,SensorType.DOOR);
    newSensor1.setActive(true);
    Sensor newSensor2 =new Sensor(random,SensorType.WINDOW);
    newSensor2.setActive(true);
    Sensor newSensor3 =new Sensor(random,SensorType.MOTION);
    newSensor3.setActive(true);
    sensors.add(newSensor1);
    sensors.add(newSensor2);
    sensors.add(newSensor3);
    when(securityRepository.getSensors()).thenReturn(sensors);
    securityService.setArmingStatus(armingStatus);
    verify(securityRepository,times(1)).setArmingStatus(armingStatus);
    verify(securityRepository,times(1)).getSensors();
    verifySensorStatus(sensors,false);
}

//unit test 11
//If the system is armed-home while the camera shows a cat, set the alarm status to alarm
@Test
void IfSystemIsArmedHome_CatDetected_SetAlarmStatusToAlarm()
{
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
    securityService.processImage(mock(BufferedImage.class));
    verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);

}

//Dummy tests
@Test
void testAddSensor(){
        securityService.addSensor(any(Sensor.class));
        verify(securityRepository,times(1)).addSensor(any());
}

@Test
void testRemoveSensor(){
        securityService.removeSensor(any(Sensor.class));
        verify(securityRepository,times(1)).removeSensor(any());
}

@Test
void testStatusListener(){
    SecurityService securityService1=new SecurityService(securityRepository);
    securityService1.addStatusListener(statusListener);

}

@Test
void test_getAlarmStatus()
{
securityService.getAlarmStatus();

}

@Test
void testRemoveListener()
{
    securityService.removeStatusListener(statusListener);
}

private void verifySensorStatus(Set<Sensor>sensors,boolean sensorStatus)
{
    sensors.forEach(sensor1 -> assertEquals(sensorStatus,sensor1.getActive()));

}

@Test
public void ifAlarmStatus_Alarm_Change_PendingAlarm(){
    Sensor testSensor = mock(Sensor.class);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
    securityService.changeSensorStatus(testSensor);
    verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

}

    @Test
    public void IfArmedHome_CatImagePresent_ChangeStatus_Alarm() {
        securityService.isCatImage = true;
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
    }
}

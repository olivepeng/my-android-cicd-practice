package com.digifly.uart;

import androidx.annotation.Nullable;

import com.digifly.uart.data.UartReceiveAckData;
import com.digifly.uart.data.UartReceiveBaseData;
import com.digifly.uart.type.UartBatModeType;

import java.time.ZonedDateTime;

import timber.log.Timber;

/**
 * 解析uart 接收資料
 */
public class ParserUartReceive {

    @Nullable
    public synchronized static UartReceiveBaseData parser(final byte[] originData) {

        if (((originData[0] & 0xff) != 0xaa) ||
                ((originData[originData.length - 1] & 0xff) != 0x55) ||
                originData.length != 9) {
            return null;
        }

        UartReceiveBaseData uartReceiveBaseData = new UartReceiveBaseData(originData);

        Byte batModeByte = null;
        try {
            batModeByte = originData[1];
        } catch (Exception e) {
            Timber.e(e);
        }

        if (batModeByte != null) {
            UartBatModeType uartBatModeType = null;
            for (UartBatModeType t : UartBatModeType.values()) {
                if (t.getValue() == batModeByte) {
                    uartBatModeType = t;
                    break;
                }
            }
            uartReceiveBaseData.setUartBatModeType(uartBatModeType);
        }

        Byte batteryHealthByte = null;
        try {
            batteryHealthByte = originData[2];
        } catch (Exception e) {
            Timber.e(e);
        }
        if (batteryHealthByte != null) {
            uartReceiveBaseData.setBatteryHealth(batteryHealthByte.intValue());
        }

        Byte batteryRsocByte = null;
        try {
            batteryRsocByte = originData[3];
        } catch (Exception e) {
            Timber.e(e);
        }
        if (batteryRsocByte != null) {
            uartReceiveBaseData.setBatteryRsoc(batteryRsocByte.intValue());
        }

        return uartReceiveBaseData;
    }

    @Nullable
    public synchronized static UartReceiveAckData parserAck(final byte[] originData) {

        if (((originData[0] & 0xff) == 0xA5) ||
                ((originData[originData.length - 1] & 0xff) == 0xAA) ) {
            if(originData.length==3){

            }else if(originData.length ==7 &&
                    ((originData[3] & 0xff) == 0xB5)){ //get MCU FW

            }else{
                return null;
            }
        }else {
            return null;
        }

        UartReceiveAckData receiveAckData = new UartReceiveAckData(originData , ZonedDateTime.now());

        Byte tinesByte = null;
        try {
            tinesByte = originData[1];
        } catch (Exception e) {
            Timber.e(e);
        }

        receiveAckData.setOk(tinesByte == 0x5a);

        return receiveAckData;
    }
}

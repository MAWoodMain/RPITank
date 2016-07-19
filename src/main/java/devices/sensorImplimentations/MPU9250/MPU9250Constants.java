package devices.sensorImplimentations.MPU9250;

/**
 * RPITank
 * Created by MAWood on 09/07/2016.
 */
enum Registers
{
    AK8963_ADDRESS   (0x0C),
    WHO_AM_I_AK8963  (0x00), // should return (0x48
    INFO             (0x01),
    AK8963_ST1       (0x02),  // data ready status bit 0
    AK8963_XOUT_L    (0x03),  // data
    AK8963_XOUT_H    (0x04),
    AK8963_YOUT_L    (0x05),
    AK8963_YOUT_H    (0x06),
    AK8963_ZOUT_L    (0x07),
    AK8963_ZOUT_H    (0x08),
    AK8963_ST2       (0x09),  // Data overflow bit 3 and data read error status bit 2
    AK8963_CNTL      (0x0A),  // Power down (0000), single-measurement (0001), self-test (1000) and Fuse ROM (1111) modes on bits 3:0
    AK8963_ASTC      (0x0C),  // Self test control
    AK8963_I2CDIS    (0x0F),  // I2C disable
    AK8963_ASAX      (0x10),  // Fuse ROM x-axis sensitivity adjustment value
    AK8963_ASAY      (0x11),  // Fuse ROM y-axis sensitivity adjustment value
    AK8963_ASAZ      (0x12),  // Fuse ROM z-axis sensitivity adjustment value

    SELF_TEST_X_GYRO (0x00),
    SELF_TEST_Y_GYRO (0x01),
    SELF_TEST_Z_GYRO (0x02),

    SELF_TEST_X_ACCEL(0x0D),
    SELF_TEST_Y_ACCEL(0x0E),
    SELF_TEST_Z_ACCEL(0x0F),

    SELF_TEST_A      (0x10),

    XG_OFFSET_H      (0x13),  // User-defined trim values for gyroscope
    XG_OFFSET_L      (0x14),
    YG_OFFSET_H      (0x15),
    YG_OFFSET_L      (0x16),
    ZG_OFFSET_H      (0x17),
    ZG_OFFSET_L      (0x18),
    SMPLRT_DIV       (0x19),
    CONFIG           (0x1A),
    GYRO_CONFIG      (0x1B),
    ACCEL_CONFIG     (0x1C),
    ACCEL_CONFIG2    (0x1D),
    LP_ACCEL_ODR     (0x1E),
    WOM_THR          (0x1F),

    MOT_DUR          (0x20),  // Duration counter threshold for motion interrupt generation, 1 kHz rate, LSB = 1 ms
    ZMOT_THR         (0x21),  // Zero-motion detection threshold bits [7:0]
    ZRMOT_DUR        (0x22),  // Duration counter threshold for zero motion interrupt generation, 16 Hz rate, LSB = 64 ms

    FIFO_EN          (0x23),
    I2C_MST_CTRL     (0x24),
    I2C_SLV0_ADDR    (0x25),
    I2C_SLV0_REG     (0x26),
    I2C_SLV0_CTRL    (0x27),
    I2C_SLV1_ADDR    (0x28),
    I2C_SLV1_REG     (0x29),
    I2C_SLV1_CTRL    (0x2A),
    I2C_SLV2_ADDR    (0x2B),
    I2C_SLV2_REG     (0x2C),
    I2C_SLV2_CTRL    (0x2D),
    I2C_SLV3_ADDR    (0x2E),
    I2C_SLV3_REG     (0x2F),
    I2C_SLV3_CTRL    (0x30),
    I2C_SLV4_ADDR    (0x31),
    I2C_SLV4_REG     (0x32),
    I2C_SLV4_DO      (0x33),
    I2C_SLV4_CTRL    (0x34),
    I2C_SLV4_DI      (0x35),
    I2C_MST_STATUS   (0x36),
    INT_PIN_CFG      (0x37),
    INT_ENABLE       (0x38),
    DMP_INT_STATUS   (0x39),  // Check DMP interrupt
    INT_STATUS       (0x3A),
    ACCEL_XOUT_H     (0x3B),
    ACCEL_XOUT_L     (0x3C),
    ACCEL_YOUT_H     (0x3D),
    ACCEL_YOUT_L     (0x3E),
    ACCEL_ZOUT_H     (0x3F),
    ACCEL_ZOUT_L     (0x40),
    TEMP_OUT_H       (0x41),
    TEMP_OUT_L       (0x42),
    GYRO_XOUT_H      (0x43),
    GYRO_XOUT_L      (0x44),
    GYRO_YOUT_H      (0x45),
    GYRO_YOUT_L      (0x46),
    GYRO_ZOUT_H      (0x47),
    GYRO_ZOUT_L      (0x48),
    EXT_SENS_DATA_00 (0x49),
    EXT_SENS_DATA_01 (0x4A),
    EXT_SENS_DATA_02 (0x4B),
    EXT_SENS_DATA_03 (0x4C),
    EXT_SENS_DATA_04 (0x4D),
    EXT_SENS_DATA_05 (0x4E),
    EXT_SENS_DATA_06 (0x4F),
    EXT_SENS_DATA_07 (0x50),
    EXT_SENS_DATA_08 (0x51),
    EXT_SENS_DATA_09 (0x52),
    EXT_SENS_DATA_10 (0x53),
    EXT_SENS_DATA_11 (0x54),
    EXT_SENS_DATA_12 (0x55),
    EXT_SENS_DATA_13 (0x56),
    EXT_SENS_DATA_14 (0x57),
    EXT_SENS_DATA_15 (0x58),
    EXT_SENS_DATA_16 (0x59),
    EXT_SENS_DATA_17 (0x5A),
    EXT_SENS_DATA_18 (0x5B),
    EXT_SENS_DATA_19 (0x5C),
    EXT_SENS_DATA_20 (0x5D),
    EXT_SENS_DATA_21 (0x5E),
    EXT_SENS_DATA_22 (0x5F),
    EXT_SENS_DATA_23 (0x60),
    MOT_DETECT_STATUS (0x61),
    I2C_SLV0_DO      (0x63),
    I2C_SLV1_DO      (0x64),
    I2C_SLV2_DO      (0x65),
    I2C_SLV3_DO      (0x66),
    I2C_MST_DELAY_CTRL (0x67),
    SIGNAL_PATH_RESET  (0x68),
    MOT_DETECT_CTRL  (0x69),
    USER_CTRL        (0x6A),  // Bit 7 enable DMP, bit 3 reset DMP
    PWR_MGMT_1       (0x6B), // Device defaults to the SLEEP mode
    PWR_MGMT_2       (0x6C),
    DMP_BANK         (0x6D),  // Activates a specific bank in the DMP
    DMP_RW_PNT       (0x6E),  // Set read/write pointer to a specific start address in specified DMP bank
    DMP_REG          (0x6F),  // Register in DMP from which to read or to which to write
    DMP_REG_1        (0x70),
    DMP_REG_2        (0x71),
    FIFO_COUNTH      (0x72),
    FIFO_COUNTL      (0x73),
    FIFO_R_W         (0x74),
    WHO_AM_I_MPU9250 (0x75), // Should return (0x71
    XA_OFFSET_H      (0x77),
    XA_OFFSET_L      (0x78),
    YA_OFFSET_H      (0x7A),
    YA_OFFSET_L      (0x7B),
    ZA_OFFSET_H      (0x7D),
    ZA_OFFSET_L      (0x7E),

    M_MODE           (0x06); // 2 for 8 Hz, 6 for 100 Hz continuous magnetometer data read

    private final int value;
    Registers(int value)
    {
        this.value = value;
    }
    public int getValue()
    {
        return value;
    }
}

enum MagScale
{
    MFS_14BIT((byte)0x00,10f*4912f/8190f), //mscale val = 0, 14 bit will be shifted 4 left
    MFS_16BIT((byte)0x01,10f*4912f/32760f); //mscale val = 1, 16 bit will be shifted 4 left

    private final byte value;
    private final float res;
    MagScale(byte value, float res)
    {
        this.value = value;
        this.res = res;
    }
    public byte getValue()
    {
        return value;
    }
    public float getRes()
    {
        return res;
    }
    public int getMinMax()
    {
        return 4800;
    }
}

enum AccScale
{
    AFS_2G(0x00,2),
    AFS_4G(0x08,4),
    AFS_8G(0x10,8),
    AFS_16G(0x18,16);

    private final int value;
    private final int minMax;
    AccScale(int value, int minMax)
    {
        this.value = value;
        this.minMax = minMax;
    }
    public byte getValue()
    {
        return (byte)value;
    }
    public double getRes()
    {
        return (double)minMax/32768.0;
    }
    public int getMinMax()
    {
        return minMax;
    }
}

enum GyrScale
{
    GFS_250DPS(0x00,250),
    GFS_500DPS(0x08,500),
    GFS_1000DPS(0x10,1000),
    GFS_2000DPS(0x18,2000);

    private final int value;
    private final int minMax;
    GyrScale(int value, int minMax)
    {
        this.value = value;
        this.minMax = minMax;
    }
    public byte getValue()
    {
        return (byte)value;
    }
    public double getRes()
    {
        return (double)minMax/32768.0;
    }
    public int getMinMax()
    {
        return minMax;
    }
}


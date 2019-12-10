package se.skl.tp.vp.util.takcache;

public class TestTakDataDefines {
    public static final String ADDRESS_1 = "address-1";
    public static final String ADDRESS_2 = "address-2";
    public static final String ADDRESS_3 = "address-3";
    public static final String RIV20 = "RIVTABP20";
    public static final String RIV21 = "RIVTABP21";
    public static final String NAMNRYMD_1 = "namnrymd-1";
    public static final String NAMNRYMD_2 = "namnrymd-2";
    public static final String RECEIVER_1 = "receiver-1";
    public static final String RECEIVER_2 = "receiver-2";
    public static final String RECEIVER_3 = "receiver-3";
    public static final String RECEIVER_4 = "receiver-4";
    public static final String SENDER_1 = "sender-1";
    public static final String SENDER_2 = "sender-2";
    public static final String SENDER_3 = "sender-3";

    public static final String RECEIVER_1_DEFAULT_RECEIVER_2 = String.format("%s#%s", RECEIVER_1, RECEIVER_2);
    public static final String RECEIVER_2_DEFAULT_RECEIVER_3 = String.format("%s#%s", RECEIVER_2, RECEIVER_3);
    public static final String RECEIVER_3_DEFAULT_RECEIVER_4 = String.format("%s#%s", RECEIVER_3, RECEIVER_4);

    public static final String AUTHORIZED_RECEIVER_IN_HSA_TREE =           "SE0000000003-1234";
    public static final String CHILD_OF_AUTHORIZED_RECEIVER_IN_HSA_TREE =  "SE0000000001-1234";
    public static final String PARENT_OF_AUTHORIZED_RECEIVER_IN_HSA_TREE = "SE0000000004-1234";

}
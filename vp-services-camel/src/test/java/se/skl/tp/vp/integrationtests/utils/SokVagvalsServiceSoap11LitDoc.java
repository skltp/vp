package se.skl.tp.vp.integrationtests.utils;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import se.skltp.tak.vagvalsinfo.wsdl.v2.HamtaAllaAnropsBehorigheterResponseType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.HamtaAllaTjanstekontraktResponseType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.HamtaAllaVirtualiseringarResponseType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.SokVagvalsInfoInterface;

@WebService(targetNamespace = "urn:skl:tp:vagvalsinfo:v2", name = "SokVagvalsSoap11LitDoc")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class SokVagvalsServiceSoap11LitDoc implements SokVagvalsInfoInterface {
    private HamtaAllaAnropsBehorigheterResponseType hamtaAllaAnropsBehorigheterResponseType = new HamtaAllaAnropsBehorigheterResponseType();
    private HamtaAllaVirtualiseringarResponseType hamtaAllaVirtualiseringarResponseType = new HamtaAllaVirtualiseringarResponseType();

    @Override
    @WebResult(name = "hamtaAllaTjanstekontraktResponse", targetNamespace = "urn:skl:tp:vagvalsinfo:v2", partName = "response")
    @WebMethod
    public HamtaAllaTjanstekontraktResponseType hamtaAllaTjanstekontrakt(@WebParam(partName = "parameters",name = "hamtaAllaTjanstekontrakt",targetNamespace = "urn:skl:tp:vagvalsinfo:v2")Object o) {
        return null;
    }

    @Override
    @WebResult(name = "hamtaAllaAnropsBehorigheterResponse", targetNamespace = "urn:skl:tp:vagvalsinfo:v2", partName = "response")
    @WebMethod
    public HamtaAllaAnropsBehorigheterResponseType hamtaAllaAnropsBehorigheter(@WebParam(partName = "parameters",name = "hamtaAllaAnropsBehorigheter",targetNamespace = "urn:skl:tp:vagvalsinfo:v2") Object o) {
        return hamtaAllaAnropsBehorigheterResponseType;
    }

    @Override
    @WebResult(name = "hamtaAllaVirtualiseringarResponse", targetNamespace = "urn:skl:tp:vagvalsinfo:v2", partName = "response")
    @WebMethod
    public HamtaAllaVirtualiseringarResponseType hamtaAllaVirtualiseringar(@WebParam(partName = "parameters",name = "hamtaAllaVirtualiseringar",targetNamespace = "urn:skl:tp:vagvalsinfo:v2") Object o) {
        return hamtaAllaVirtualiseringarResponseType;
    }

}
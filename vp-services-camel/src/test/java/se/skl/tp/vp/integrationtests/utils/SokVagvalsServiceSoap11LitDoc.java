package se.skl.tp.vp.integrationtests.utils;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import se.skltp.tak.vagvalsinfo.wsdl.v2.*;

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

    @Override
    public HamtaAllaTjanstekomponenterResponseType hamtaAllaTjanstekomponenter(Object parameters) {
        return null;
    }

}
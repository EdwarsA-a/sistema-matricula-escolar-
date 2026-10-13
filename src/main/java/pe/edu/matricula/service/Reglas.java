package pe.edu.matricula.service;
import java.time.LocalDate;
import java.util.Set;
public final class Reglas {
    private Reglas(){}
    public static String required(String value,int max,String label){
        String v=value==null?"":value.strip();
        if(v.isBlank()||v.length()>max)throw new BusinessException(label+": ingrese de 1 a "+max+" caracteres.");
        return v;
    }
    public static String optional(String value,int max,String label){
        String v=value==null?"":value.strip();if(v.length()>max)throw new BusinessException(label+": máximo "+max+" caracteres.");return v.isEmpty()?null:v;
    }
    public static long id(String value,String label){
        try{long id=Long.parseLong(value);if(id<=0)throw new NumberFormatException();return id;}
        catch(Exception e){throw new BusinessException("Seleccione "+label+" válido.");}
    }
    public static LocalDate date(String value,String label){
        try{return LocalDate.parse(value);}catch(Exception e){throw new BusinessException(label+": ingrese una fecha válida.");}
    }
    public static void ensure(boolean condition,String message){if(!condition)throw new BusinessException(message);}
    public static void capacity(long occupied,long capacity){ensure(occupied<capacity,"La sección no tiene vacantes disponibles.");}
    public static void transition(String state,String action){
        boolean valid=switch(action){
            case "ACTIVACION"->"PENDIENTE".equals(state);
            case "CAMBIO_SECCION","RETIRO","TRASLADO_EXTERNO"->"ACTIVA".equals(state);
            case "ANULACION"->Set.of("PENDIENTE","ACTIVA").contains(state);
            default->false;
        };
        ensure(valid,"La operación no corresponde al estado actual de la matrícula.");
    }
}

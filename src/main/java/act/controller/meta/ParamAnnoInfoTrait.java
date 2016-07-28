package act.controller.meta;

public interface ParamAnnoInfoTrait {
    int index();
    boolean compatibleWith(ParamAnnoInfoTrait otherParamAnnotation);
    String compatibilityErrorMessage(ParamAnnoInfoTrait otherParamAnnotation);
    void attachTo(HandlerParamMetaInfo param);
}

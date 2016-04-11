# Creating uniform scanner/enhancer architecture

## Overview
 
|         |Action handler|Mailer|Commander|Job|Event handler|
|---------|--------------|------|---------|---|-------------|
| Scanner |[ControllerByteCodeScanner](#controller-scanner)|MailerByteCodeScanner|CommanderByteCodeScanner|JobByteCodeScanner|N/A |
| | | | | | | |

### Scanner

#### <a name="controller-scanner"></a>ControllerByteCodeScanner

* Check if a class is a controller
* Check if there are `ActionContext` field (obsolete)
* Check `@With` and `@Controller` annotations
* Check [eligible](#controller-action-handler-eligibility) methods
    * Check `@XxxAction` annotations
        * Register route mapping
    * Check interceptor annotations (`Before`, `After`, `Finally`, `Catch`)
    * Check `PropSpec` annotation
    * Populate [ControllerClassMetaInfo](#ControllerClassMetaInfo)


### Miscs

#### Eligible methods

* <a name="controller-action-handler-eligibility"></a>Controller
    * public
    * non-abstract
    * non-constructor 
    
#### Model classes

##### <a name="ControllerClassMetaInfo"></a>ControllerClassMetaInfo

* Type type
* Type superType
* boolean isAbstract
* String ctxField (obsolete)
* boolean ctxFieldIsPrivate (obsolete)
* Set`<`String`>` withList
* List`<`[ActionMethodMetaInfo](#ActionMethodMetaInfo)`>` actions

#### <a name="ActionMethodMetaInfo"></a>ActionMethodMetaInfo
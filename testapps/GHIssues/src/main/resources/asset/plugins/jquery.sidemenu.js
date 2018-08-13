/**
 * EasyUI for jQuery 1.5.5.5
 * 
 * Copyright (c) 2009-2018 www.jeasyui.com. All rights reserved.
 *
 * Licensed under the freeware license: http://www.jeasyui.com/license_freeware.php
 * To use it on other terms please contact us: info@jeasyui.com
 *
 */
(function($){
var _1=1;
function _2(_3){
$(_3).addClass("sidemenu");
};
function _4(_5,_6){
var _7=$(_5).sidemenu("options");
if(_6){
$.extend(_7,{width:_6.width,height:_6.height});
}
$(_5)._size(_7);
$(_5).find(".accordion").accordion("resize");
};
function _8(_9,_a,_b){
var _c=$(_9).sidemenu("options");
var tt=$("<ul class=\"sidemenu-tree\"></ul>").appendTo(_a);
tt.tree({data:_b,animate:_c.animate,onBeforeSelect:function(_d){
if(_d.children){
return false;
}
},onSelect:function(_e){
_12(_9,_e.id);
},onExpand:function(_f){
_22(_9,_f);
},onCollapse:function(_10){
_22(_9,_10);
},onClick:function(_11){
if(_11.children){
if(_11.state=="open"){
$(_11.target).addClass("tree-node-nonleaf-collapsed");
}else{
$(_11.target).removeClass("tree-node-nonleaf-collapsed");
}
$(this).tree("toggle",_11.target);
}
}});
tt.unbind(".sidemenu").bind("mouseleave.sidemenu",function(){
$(_a).trigger("mouseleave");
});
_12(_9,_c.selectedItemId);
};
function _13(_14,_15,_16){
var _17=$(_14).sidemenu("options");
$(_15).tooltip({content:$("<div></div>"),position:_17.floatMenuPosition,valign:"top",data:_16,onUpdate:function(_18){
var _19=$(this).tooltip("options");
var _1a=_19.data;
_18.accordion({width:_17.floatMenuWidth,multiple:false}).accordion("add",{title:_1a.text,iconCls:_1a.iconCls,collapsed:false,collapsible:false});
_8(_14,_18.accordion("panels")[0],_1a.children);
},onShow:function(){
var t=$(this);
var tip=t.tooltip("tip").addClass("sidemenu-tooltip");
tip.children(".tooltip-content").addClass("sidemenu");
tip.find(".accordion").accordion("resize");
tip.unbind().bind("mouseenter",function(){
t.tooltip("show");
}).bind("mouseleave",function(){
t.tooltip("hide");
});
},onPosition:function(){
if(!_17.collapsed){
$(this).tooltip("tip").css({left:-999999});
}
}});
};
function _1b(_1c,_1d){
$(_1c).find(".sidemenu-tree").each(function(){
_1d($(this));
});
$(_1c).find(".tooltip-f").each(function(){
var tip=$(this).tooltip("tip");
if(tip){
tip.find(".sidemenu-tree").each(function(){
_1d($(this));
});
}
});
};
function _12(_1e,_1f){
var _20=$(_1e).sidemenu("options");
_1b(_1e,function(t){
t.find("div.tree-node-selected").removeClass("tree-node-selected");
var _21=t.tree("find",_1f);
if(_21){
$(_21.target).addClass("tree-node-selected");
_20.selectedItemId=_21.id;
t.trigger("mouseleave");
_20.onSelect.call(_1e,_21);
}
});
};
function _22(_23,_24){
_1b(_23,function(t){
var _25=t.tree("find",_24.id);
if(_25){
t.tree(_24.state=="open"?"expand":"collapse",_25.target);
}
});
};
function _26(_27){
var _28=$(_27).sidemenu("options");
$(_27).empty();
if(_28.data){
$.easyui.forEach(_28.data,true,function(_29){
if(!_29.id){
_29.id="_easyui_sidemenu_"+(_1++);
}
if(!_29.iconCls){
_29.iconCls="sidemenu-default-icon";
}
if(_29.children){
_29.nodeCls="tree-node-nonleaf";
if(!_29.state){
_29.state="closed";
}
if(_29.state=="open"){
_29.nodeCls="tree-node-nonleaf";
}else{
_29.nodeCls="tree-node-nonleaf tree-node-nonleaf-collapsed";
}
}
});
var acc=$("<div></div>").appendTo(_27);
acc.accordion({fit:_28.height=="auto"?false:true,border:_28.border,multiple:_28.multiple});
for(var i=0;i<data.length;i++){
acc.accordion("add",{title:data[i].text,selected:data[i].state=="open",iconCls:data[i].iconCls});
var ap=acc.accordion("panels")[i];
_8(_27,ap,data[i].children);
_13(_27,ap.panel("header"),data[i]);
}
}
};
function _2a(_2b,_2c){
var _2d=$(_2b).sidemenu("options");
_2d.collapsed=_2c;
var acc=$(_2b).find(".accordion");
var _2e=acc.accordion("panels");
acc.accordion("options").animate=false;
if(_2d.collapsed){
$(_2b).addClass("sidemenu-collapsed");
for(var i=0;i<_2e.length;i++){
var _2f=_2e[i];
if(_2f.panel("options").collapsed){
_2d.data[i].state="closed";
}else{
_2d.data[i].state="open";
acc.accordion("unselect",i);
}
var _30=_2f.panel("header");
_30.find(".panel-title").html("");
_30.find(".panel-tool").hide();
}
}else{
$(_2b).removeClass("sidemenu-collapsed");
for(var i=0;i<_2e.length;i++){
var _2f=_2e[i];
if(_2d.data[i].state=="open"){
acc.accordion("select",i);
}
var _30=_2f.panel("header");
_30.find(".panel-title").html(_2f.panel("options").title);
_30.find(".panel-tool").show();
}
}
acc.accordion("options").animate=_2d.animate;
};
function _31(_32){
$(_32).find(".tooltip-f").each(function(){
$(this).tooltip("destroy");
});
$(_32).remove();
};
$.fn.sidemenu=function(_33,_34){
if(typeof _33=="string"){
var _35=$.fn.sidemenu.methods[_33];
return _35(this,_34);
}
_33=_33||{};
return this.each(function(){
var _36=$.data(this,"sidemenu");
if(_36){
$.extend(_36.options,_33);
}else{
_36=$.data(this,"sidemenu",{options:$.extend({},$.fn.sidemenu.defaults,$.fn.sidemenu.parseOptions(this),_33)});
_2(this);
}
_4(this);
_26(this);
_2a(this,_36.options.collapsed);
});
};
$.fn.sidemenu.methods={options:function(jq){
return jq.data("sidemenu").options;
},resize:function(jq,_37){
return jq.each(function(){
_4(this,_37);
});
},collapse:function(jq){
return jq.each(function(){
_2a(this,true);
});
},expand:function(jq){
return jq.each(function(){
_2a(this,false);
});
},destroy:function(jq){
return jq.each(function(){
_31(this);
});
}};
$.fn.sidemenu.parseOptions=function(_38){
var t=$(_38);
return $.extend({},$.parser.parseOptions(_38,["width","height"]));
};
$.fn.sidemenu.defaults={width:200,height:"auto",border:true,animate:true,multiple:true,collapsed:false,data:null,floatMenuWidth:200,floatMenuPosition:"right",onSelect:function(_39){
}};
})(jQuery);


document.addEventListener("DOMContentLoaded", function(event) {

var act_captchaDock = document.getElementById('act_captcha_dock');
if (!act_captchaDock) {
throw "cannot find act_captcha_dock element";
}
var act_onCaptcha = function(captcha) {
var top = document.getElementById('act_captcha_dock_inner');
if (top) {
  act_captchaDock.removeChild(top);
}

top = document.createElement('div');
top.id = 'act_captcha_dock_inner';
act_captchaDock.appendChild(top);


var divTitle = document.createElement('div');
divTitle.classList.add('act_captcha_title');
divTitle.innerText = captcha.caption;
top.appendChild(divTitle);

var imgCaptcha = document.createElement('img');
imgCaptcha.classList.add('act_captcha_img');
imgCaptcha.setAttribute('src', captcha.mediaUrl);
top.appendChild(imgCaptcha);

var divInstruction = document.createElement('div');
divInstruction.classList.add('act_captcha_instruction');
divInstruction.innerText = captcha.instruction
top.appendChild(divInstruction);

var divContainer = document.createElement('div');
divContainer.classList.add('act_captcha_input_container');
top.appendChild(divContainer);

var inputToken = document.createElement('input');
inputToken.setAttribute('name', 'a-captcha-token');
inputToken.setAttribute('type', 'hidden');
inputToken.setAttribute('value', captcha.token);
divContainer.appendChild(inputToken);

var inputAnswer = document.createElement('input');
inputAnswer.setAttribute('name', 'a-captcha-answer');
divContainer.appendChild(inputAnswer);

var btnRefresh = document.createElement('img');
btnRefresh.classList.add('act_captcha_icon');
btnRefresh.setAttribute('src', '/~/asset/img/refresh.png');
divContainer.appendChild(btnRefresh);
btnRefresh.onclick=act_loadCaptcha;
}

var httpRequest = new XMLHttpRequest();
httpRequest.onreadystatechange = function() {
if (httpRequest.readyState === 4) {
  if (httpRequest.status === 200) {
    var captcha = JSON.parse(httpRequest.responseText);
    if (typeof act_captcha_handler === 'function') {
      act_captcha_handler(captcha);
    } else {
      act_onCaptcha(captcha);
    }
  } else {
    console.warn("error getting captcha session data");
  }
}
};

function act_loadCaptcha() {
  httpRequest.open('GET', '/~/captcha');
  httpRequest.send();
}

act_loadCaptcha();

});


var exec = require('cordova/exec');
    
var Richa={};
Richa.start=function(arg0, success, error){
    exec(success, error, 'Richa', 'start', [arg0])
}

module.exports=Richa

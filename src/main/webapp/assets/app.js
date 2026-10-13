'use strict';
document.querySelectorAll('form[data-confirm]').forEach(form=>form.addEventListener('submit',event=>{if(!window.confirm(form.dataset.confirm))event.preventDefault();}));
document.querySelectorAll('[data-print]').forEach(button=>button.addEventListener('click',()=>window.print()));
const period=document.getElementById('periodo-select'),section=document.getElementById('seccion-select');
if(period&&section){const filter=()=>{section.value='';for(const option of section.options){if(option.value){const show=option.dataset.periodo===period.value;option.hidden=!show;option.disabled=!show;}}};period.addEventListener('change',filter);filter();}

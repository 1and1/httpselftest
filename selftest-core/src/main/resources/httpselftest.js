//
document.querySelector('body').classList.add('js')
document.querySelector('body').onclick = function(evt) {

  // test case collapsing
  var closestH2 = evt.target;
  while (closestH2 != null && closestH2.nodeName != 'H2') {
    closestH2 = closestH2.parentNode;
  }
  if (closestH2 != null && closestH2.nodeName === 'H2' && closestH2.parentNode.classList.contains('group')) {
    var group = closestH2.parentNode;
    if (group.classList.contains('open')) {
      group.classList.remove('open');
    } else {
      group.classList.add('open');
    }
  }

  // http presentation switching
  if (evt.target.classList.contains('presentationToggle')) {
    var toggleNode = evt.target;
    var presenterId = toggleNode.textContent;
    var headerNode = toggleNode.parentNode; // ^ h3
    var httpNode = headerNode.nextElementSibling; // ~ div

    headerNode.querySelectorAll('.active').forEach(function(child) {
      child.classList.remove('active');
    });
    httpNode.querySelectorAll('.active').forEach(function(child) {
      child.classList.remove('active');
    });
    toggleNode.classList.add('active');
    httpNode.querySelector('.presenterContent.' + presenterId).classList.add('active');
  }
}

function resize() {
  // JavaScript is running, so convert page from vertical scrolling to
  // horizontal paging:
  $('#frame').addClass('js');
  $('#columns').addClass('js');
  $(document.body).height($(window).height() - 20);

  // Measure total width of the columns and make #wide exactly that big.
  $('#wide').css('width', $('#frame')[0].scrollWidth + 'px');

  // Insert per-column headers and footers
  $('#columns').before('<h1 id="headrow">Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header Header </h1>');
  $('#columns').after('<div id="footrow">footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer</div>');
  $('#columns').height(
      $('#frame').innerHeight()
      - $('#headrow').outerHeight()
      - $('#footrow').outerHeight());
}

function scrollBy(p) {
  var frame = $('#frame')[0];
  var width = $('#frame').innerWidth(); // not quite right
  frame.scrollLeft = (Math.floor(frame.scrollLeft / width) + p) * width;
}

$(document).keydown(function(e) {
  if(e.which == 39 || e.which == 32) {
    scrollBy(1);
  }
  if(e.which == 37) {
    scrollBy(-1);
  }
});

$(document).ready(resize);
//$(document).resize(resize);

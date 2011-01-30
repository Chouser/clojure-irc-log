function resize() {
  // JavaScript is running, so convert page from vertical scrolling to
  // horizontal paging:
  $('#frame').addClass('paged');
  $('#columns').addClass('paged');
  $(document.body).height($(window).height() - 20);

  // Insert initial per-column header and footer to establish their vertical
  // size so the page_count can be calculated correctly
  $('#columns').before('<div id="headrow"></div>');
  var header = $('.header');
  $('#headrow').append(header);

  $('#columns').after('<div id="footrow">footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer footer</div>');
  $('#headrow').height($('#headrow').outerHeight());
  $('#columns').height(
      $('#frame').innerHeight()
      - $('#headrow').outerHeight()
      - $('#footrow').outerHeight());

  // Measure widths
  var doc_width = window.col_width = $('#frame')[0].scrollWidth;
  var col_width = window.col_width = $('#frame').innerWidth() - 1; //not quite right
  var page_count = Math.ceil(doc_width / col_width);

  // Make #wide exactly as wide as all the columns it contains
  // This not only sets up the container for headers and footers, but works
  // around a rendering bug in chrome
  $('#wide').css('width', doc_width + 'px');

  // Insert per-column headers and footers
  $('#headrow').addClass('paged');
  for(var i = 2; i < page_count; ++i) {
    $('#headrow').append(header.clone());
  }
}

function scrollBy(p) {
  var width = window.col_width;
  var frame = $('#frame')[0];
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

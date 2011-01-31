function resize() {
  // JavaScript is running, so convert page from vertical scrolling to
  // horizontal paging:
  $('#frame').addClass('paged');
  $('#columns').addClass('paged');
  $(document.body).height($(window).height() - 20);

  // Insert initial per-column header and footer to establish their vertical
  // size so the page_count can be calculated correctly
  $('#columns').before('<header><table class="page_head"><tr><td class="page_head_left"></td><td class="page_head_right"></td></tr></table></header>');
  $('.page_head_left' ).append($('h1').contents().clone());
  $('.page_head_right').append($('h2').contents().clone());
  $('h1').css('margin-top', 0);
  var page_head = $('.page_head').clone();
  $('.page_head').css('visibility', 'hidden');

  $('#columns').after('<footer><table class="page_foot"><tr><td class="page_foot_left"><button onclick="scrollBy(-1)">&lt;&lt;&lt;</button></td><td class="page_foot_mid"></td><td class="page_foot_right"><button onclick="scrollBy(1)">&gt;&gt;&gt;</button></td></tr></table></footer>');
  var page_foot = $('.page_foot');
  $('header').height($('header').outerHeight());
  $('#columns').height(
      $('#frame').innerHeight()
      - $('header').outerHeight()
      - $('footer').outerHeight());

  // Measure widths
  var doc_width = window.doc_width = $('#frame')[0].scrollWidth;
  var col_width = window.col_width = $('#frame').innerWidth() - 1; //not quite right
  var page_count = Math.ceil(doc_width / col_width);

  // Make #wide exactly as wide as all the columns it contains
  // This not only sets up the container for headers and footers, but works
  // around a rendering bug in chrome
  $('#wide').css('width', doc_width + 'px');

  // Insert per-column headers and footers
  for(var i = 2; i < page_count; ++i) {
    $('header').append(page_head.clone());
    $('footer').append(page_foot.clone());
  }

  // Fix page numbers
  $('.page_foot_mid').each(function(i, elem) {
    elem.innerHTML = 'page ' + (i+1) + ' of ' + (page_count-1);
  });
}

function scrollBy(p) {
  var width = window.col_width;
  var frame = $('#frame')[0];
  var page_count = Math.ceil(window.doc_width / width);
  frame.scrollLeft = Math.min(page_count-2, (Math.floor(frame.scrollLeft / width) + p)) * width;
}

$(document).keydown(function(e) {
  if(e.which == 39 || e.which == 32 || e.which == 34 || e.which == 40) {
    scrollBy(1);
  }
  if(e.which == 37 || e.which == 33 || e.which == 38) {
    scrollBy(-1);
  }
});

$(document).ready(resize);
//$(document).resize(resize);

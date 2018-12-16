function showHide(callPoints, listIndex) {
   var tableCell = callPoints.parentNode;

   if (listIndex >= 0) {
      tableCell = tableCell.parentNode;
   }
   else {
      listIndex = 0;
   }

   var list = tableCell.getElementsByTagName('ol')[listIndex].style;
   list.margin = "1ex 2em 1ex " + (4 + listIndex) + "em";
   list.display = list.display == 'none' ? 'block' : 'none';
}

var allFilesShown = true;
function showHideAllFiles() {
   allFilesShown = !allFilesShown;
   var newDisplay = allFilesShown ? 'block' : 'none';
   var rows = document.getElementById('packages').rows;

   for (var i = 0; i < rows.length; i++) {
      var filesCell = rows[i].cells[1];
      filesCell.style.display = newDisplay;
   }
}

function showHideFiles(files) {
   var filesCell = files.parentNode.cells[1];
   var filesTable = filesCell.getElementsByTagName('table')[0];
   var fileCount = filesCell.getElementsByTagName('span')[0];
   var filesShown = filesCell.style.display != 'none' && filesTable.style.display != 'none';

   if (filesShown) {
      filesTable.style.display = 'none';
      fileCount.style.display = 'block';
   }
   else {
      fileCount.style.display = 'none';
      filesTable.style.display = 'block';
   }
}

function showHideLines(cell) {
   var content = cell.children;
   var expanded = content[0].style;
   var collapsed = content[1].style;
   var showingExpanded = expanded.display == 'block';
   expanded.display = showingExpanded ? 'none' : 'block';
   collapsed.display = showingExpanded ? 'block' : 'none';
}

var metricCol;
function rowOrder(r1, r2) {
   var c1 = r1.cells[metricCol];
   var c2 = r2.cells[metricCol];

   if (!c1 || !c2) {
      return -1;
   }

   var t1 = c1.title;
   var t2 = c2.title;

   if (t1 && t2) {
      var s1 = t1.split('/')[1];
      var s2 = t2.split('/')[1];
      return s1 - s2;
   }

   return t1 ? 1 : -1;
}

function sortRows(tbl) {
   var startRow = 0;
   var endRow = tbl.rows.length;

   if (tbl.id == 'packages') {
      metricCol = 1;
      startRow = 1;
      endRow--;
   }
   else {
      metricCol = 0;
   }

   var rs = new Array();

   for (var i = startRow; i < endRow; i++) {
      rs[i - startRow] = tbl.rows[i];
   }

   rs.sort(rowOrder);

   for (var i = 0; i < rs.length; i++) {
      rs[i] = rs[i].innerHTML;
   }

   for (var i = 0; i < rs.length; i++) {
      tbl.rows[startRow + i].innerHTML = rs[i];
   }
}

function sortTables() {
   var tables = document.getElementsByTagName("table");

   for (var i = 0; i < tables.length; i++) {
      sortRows(tables[i]);
   }
}
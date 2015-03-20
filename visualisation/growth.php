<?php
    
    require_once("db.php");
    $type = "ARSON";
    if(isset($_GET['type']))
        $type = $_GET['type'];
    include 'header.php';
    
?>
    <style>

        #container {
            margin-left: -40px;
            height: 506px;
        }         

        text {
          font: 10px sans-serif;
        }

        .dot {
          stroke: #000;
        }

        .axis path, .axis line {
          fill: none;
          stroke: #000;
          shape-rendering: crispEdges;
        }

        .label {
          fill: #777;
        }

        .year.label {
          font: 500 196px "Helvetica Neue";
          fill: #ddd;
        }

        .year.label.active {
          fill: #aaa;
        }

        .overlay {
          fill: none;
          pointer-events: all;
          cursor: ew-resize;
        }
    </style>
    <body>
        <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <div class="container-fluid">
            <!-- Brand and toggle get grouped for better mobile display -->
                <div class="navbar-header">
                    <a class="navbar-brand" href="index.php">Spatium</a>
                </div>
                <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                    <ul class="nav navbar-nav">
                        <li class="active"><a href="#">Data Summary<span class="sr-only">(current)</span></a></li>
                        <li><a href="#">Link</a></li>
                    </ul>
                    <ul class="nav navbar-nav navbar-right">
                        <li><a href="https://github.com/shagunsodhani/spatium">Code</a></li>
                    </ul>
                </div><!-- /.navbar-collapse -->
            </div><!-- /.container-fluid -->
        </nav>

        <div class="container-fluid">
            <div class="row">
                <div class="col-sm-3 col-md-2 sidebar">
                    <?php 
                        include 'sidebar.php';
                    ?>
                </div>
                <div class="col-sm-9 col-sm-offset-2 col-md-9 col-md-offset-2 main">
                    <div id = "container" style="max-width: 800px; height: 400px; margin: 0 auto">
                    </div>
                </div>
                <div class="col-sm-3 col-md-2 col-sm-offset-10 sidebar">
                    <?php 
                    include 'types.php';
                    ?>
                </div>
            </div>
        </div>
        <script src="bootstrap/js/jquery-1.11.2.min.js"></script>
        <script src="bootstrap/js/bootstrap.min.js"></script>
        <script src="bootstrap/js/highcharts.js"></script>
        <script src="bootstrap/js/highcharts_modules.js"></script>
        <script src="bootstrap/js/data_summary.js"></script>
        <script src="bootstrap/js/d3.v3.min.js"></script>
        <script type="text/javascript">
    // various accessors that specify the four dimensions of data to visualize.
    function x(d) { return d.income; }
    function y(d) { return d.lifeExpectancy; }
    function radius(d) { return d.population; }
    function color(d) { return d.region; }
    function key(d) { return d.name; }

    // Chart dimensions.
    var margin = {top: 19.5, right: 19.5, bottom: 19.5, left: 39.5},
        width = 960 - margin.right,
        height = 500 - margin.top - margin.bottom;

    // Various scales. These domains make assumptions of data, naturally.
    var xScale = d3.scale.log().domain([300, 1e5]).range([0, width]),
        yScale = d3.scale.linear().domain([10, 85]).range([height, 0]),
        radiusScale = d3.scale.sqrt().domain([0, 5e8]).range([0, 40]),
        colorScale = d3.scale.category10();

    // The x & y axes.
    var xAxis = d3.svg.axis().orient("bottom").scale(xScale).ticks(12, d3.format(",d")),
        yAxis = d3.svg.axis().scale(yScale).orient("left");

    // Create the SVG container and set the origin.
    var svg = d3.select("#container").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
      .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    // Add the x-axis.
    svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    // Add the y-axis.
    svg.append("g")
        .attr("class", "y axis")
        .call(yAxis);

    // Add an x-axis label.
    svg.append("text")
        .attr("class", "x label")
        .attr("text-anchor", "end")
        .attr("x", width)
        .attr("y", height - 6)
        .text("income per capita, inflation-adjusted (dollars)");

    // Add a y-axis label.
    svg.append("text")
        .attr("class", "y label")
        .attr("text-anchor", "end")
        .attr("y", 6)
        .attr("dy", ".75em")
        .attr("transform", "rotate(-90)")
        .text("life expectancy (years)");

    // Add the year label; the value is set on transition.
    var label = svg.append("text")
        .attr("class", "year label")
        .attr("text-anchor", "end")
        .attr("y", height - 24)
        .attr("x", width)
        .text(1800);

    // Load the data.
    d3.json("nations.json", function(nations) {

      // A bisector since many nation's data is sparsely-defined.
      var bisect = d3.bisector(function(d) { return d[0]; });

      // Add a dot per nation. Initialize the data at 1800, and set the colors.
      var dot = svg.append("g")
          .attr("class", "dots")
        .selectAll(".dot")
          .data(interpolateData(1800))
        .enter().append("circle")
          .attr("class", "dot")
          .style("fill", function(d) { return colorScale(color(d)); })
          .call(position)
          .sort(order);

      // Add a title.
      dot.append("title")
          .text(function(d) { return d.name; });

      // Add an overlay for the year label.
      var box = label.node().getBBox();

      var overlay = svg.append("rect")
            .attr("class", "overlay")
            .attr("x", box.x)
            .attr("y", box.y)
            .attr("width", box.width)
            .attr("height", box.height)
            .on("mouseover", enableInteraction);

      // Start a transition that interpolates the data based on year.
      svg.transition()
          .duration(30000)
          .ease("linear")
          .tween("year", tweenYear)
          .each("end", enableInteraction);

      // Positions the dots based on data.
      function position(dot) {
        dot .attr("cx", function(d) { return xScale(x(d)); })
            .attr("cy", function(d) { return yScale(y(d)); })
            .attr("r", function(d) { return radiusScale(radius(d)); });
      }

      // Defines a sort order so that the smallest dots are drawn on top.
      function order(a, b) {
        return radius(b) - radius(a);
      }

      // After the transition finishes, you can mouseover to change the year.
      function enableInteraction() {
        var yearScale = d3.scale.linear()
            .domain([1800, 2009])
            .range([box.x + 10, box.x + box.width - 10])
            .clamp(true);

        // Cancel the current transition, if any.
        svg.transition().duration(0);

        overlay
            .on("mouseover", mouseover)
            .on("mouseout", mouseout)
            .on("mousemove", mousemove)
            .on("touchmove", mousemove);

        function mouseover() {
          label.classed("active", true);
        }

        function mouseout() {
          label.classed("active", false);
        }

        function mousemove() {
          displayYear(yearScale.invert(d3.mouse(this)[0]));
        }
      }

      // Tweens the entire chart by first tweening the year, and then the data.
      // For the interpolated data, the dots and label are redrawn.
      function tweenYear() {
        var year = d3.interpolateNumber(1800, 2009);
        return function(t) { displayYear(year(t)); };
      }

      // Updates the display to show the specified year.
      function displayYear(year) {
        dot.data(interpolateData(year), key).call(position).sort(order);
        label.text(Math.round(year));
      }

      // Interpolates the dataset for the given (fractional) year.
      function interpolateData(year) {
        return nations.map(function(d) {
          return {
            name: d.name,
            region: d.region,
            income: interpolateValues(d.income, year),
            population: interpolateValues(d.population, year),
            lifeExpectancy: interpolateValues(d.lifeExpectancy, year)
          };
        });
      }

      // Finds (and possibly interpolates) the value for the specified year.
      function interpolateValues(values, year) {
        var i = bisect.left(values, year, 0, values.length - 1),
            a = values[i];
        if (i > 0) {
          var b = values[i - 1],
              t = (year - a[0]) / (b[0] - a[0]);
          return a[1] * (1 - t) + b[1] * t;
        }
        return a[1];
      }
    });

    </script>


    </body>
</html>

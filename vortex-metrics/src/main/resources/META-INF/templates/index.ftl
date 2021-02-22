<#setting number_format="#">
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Vortex Metrics</title>
<link rel="shortcut icon" href="#"/>
<script type="text/javascript">
	var $contextPath = '${contextPath}';
</script>
<link href="${contextPath}/static/css/base.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="${contextPath}/static/js/lib/jquery-1.7.1.min.js"></script>
<script type="text/javascript" src="${contextPath}/static/js/lib/json2.js"></script>
<script type="text/javascript" src="${contextPath}/static/js/lib/map.js"></script>
<script src="https://code.highcharts.com/highcharts.js"></script>
<script src="https://code.highcharts.com.cn/highcharts/highcharts-more.js"></script>
<script src="https://code.highcharts.com/modules/exporting.js"></script>
<script src="https://code.highcharts.com.cn/highcharts/modules/solid-gauge.js"></script>
<script src="https://img.hcharts.cn/highcharts-plugins/highcharts-zh_CN.js"></script>
<script src="https://code.highcharts.com/themes/dark-unica.js"></script>
</head>
<script>
	var map = new Map();
	
	var mark = null;

	$(function(){
		$('#searchBtn').click(function(){
			if(mark != null){
				clearInterval(mark);
			}
			doSearch();
			mark = setInterval(doSearch, 3000);
		});
	});
	
	function doSearch(){
			var location = $.trim($('#apiLocation').val());
			if(location == null || location.length == 0){
				return;
			}
			$.get(location, function(data){
				var dataType = data.dataType;
				var name = data.name;
				var metric = data.metric;
				var entries = data.data;
				var categories = [], count=[], highestValues=[], middleValues=[], lowestValues=[];
				for(var category in entries){
					categories.push(category);
					count.push(entries[category]['count']);
					highestValues.push(entries[category]['highestValue']);
					middleValues.push(entries[category]['middleValue']);
					lowestValues.push(entries[category]['lowestValue']);
				}
				showChart('chartBox', '[Summary] dataType: ' + dataType + ', name: ' + name + ', metric: '+ metric, categories, count, highestValues, middleValues, lowestValues);
			});
	}
	
	function showChart(divId, title, categories, count, highestValues, middleValues, lowestValues){
		if(map.containsKey(divId)){
			var chart = map.get(divId);
			var data = [{
						name: 'Count',
						type: 'column',
						yAxis: 0,
						data: count,
						tooltip: {
							valueSuffix: ''
						}
					}, {
							name: 'Highest Value',
							type: 'spline',
							yAxis: 1,
							data: highestValues,
							tooltip: {
								valueSuffix: ''
							}
					}, {
							name: 'Middle Value',
							type: 'spline',
							yAxis: 2,
							data: middleValues,
							dashStyle: 'LongDash',
							tooltip: {
								valueSuffix: ''
							}
					}, {
						name: 'Lowest Value',
						type: 'spline',
						yAxis: 3,
						data: lowestValues,
						dashStyle: 'Dash',
						tooltip: {
							valueSuffix: ''
						}
					}];
			chart.update({
				xAxis: [{
					categories: categories,
					crosshair: true
				}],
				series: data
			});
		}else{
			var chart = Highcharts.chart(divId, {
				chart: {
						zoomType: 'xy'
				},
				title: {
						text: title
				},
				xAxis: [{
						categories: categories,
						crosshair: true
				}],
				yAxis: [{
						labels: {
								format: '{value}',
								style: {
										color: Highcharts.getOptions().colors[1]
								}
						},
						title: {
								text: 'Highest Value',
								style: {
										color: Highcharts.getOptions().colors[1]
								}
						},
						opposite: true
						}, {
								gridLineWidth: 0,
								title: {
										text: 'Count',
										style: {
												color: Highcharts.getOptions().colors[0]
										}
								},
								labels: {
										format: '{value}',
										style: {
												color: Highcharts.getOptions().colors[0]
										}
								}
						}, {
							gridLineWidth: 0,
							title: {
									text: 'Middle Value',
									style: {
											color: Highcharts.getOptions().colors[2]
									}
							},
							labels: {
									format: '{value}',
									style: {
											color: Highcharts.getOptions().colors[2]
									}
							},
							opposite: true
						},  {
							gridLineWidth: 0,
							title: {
									text: 'Lowest Value',
									style: {
											color: Highcharts.getOptions().colors[3]
									}
							},
							labels: {
									format: '{value}',
									style: {
											color: Highcharts.getOptions().colors[3]
									}
							},
							opposite: true
						}],
				tooltip: {
						shared: true
				},
				legend: {
						layout: 'vertical',
						align: 'right',
						x: -200,
						verticalAlign: 'top',
						y: 0,
						floating: true,
						backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'
				},
				series: [{
						name: 'Count',
						type: 'column',
						yAxis: 0,
						data: count,
						tooltip: {
							valueSuffix: ''
						}
				}, {
						name: 'Highest Value',
						type: 'spline',
						yAxis: 1,
						data: highestValues,
						tooltip: {
							valueSuffix: ''
						}
				},{
					name: 'Middle Value',
					type: 'spline',
					yAxis: 2,
					data: middleValues,
					dashStyle: 'LongDash',
					tooltip: {
						valueSuffix: ''
					}
				},{
					name: 'Lowest Value',
					type: 'spline',
					yAxis: 3,
					data: lowestValues,
					dashStyle: 'Dash',
					tooltip: {
						valueSuffix: ''
					}
				}]
			});
			map.put(divId, chart);
		}
		
	}
</script>
<body>
	<div id="top">
	</div>
	<div id="container">
		<div id="searchBox">
			<label>Location: </label><input type="text" id="apiLocation"/>
			<input type="button" value="Search" id="searchBtn"/>
		</div>
		<div id="chartBox">
		</div>
	</div>
	<div id="foot">
		Atlantis Framework 1.0
	</div>
</body>
</html>
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
<script src="https://code.highcharts.com/highcharts-more.js"></script>
<script src="https://code.highcharts.com/modules/solid-gauge.js"></script>
<script src="https://code.highcharts.com/modules/exporting.js"></script>
<script src="https://code.highcharts.com/modules/export-data.js"></script>
<script src="https://code.highcharts.com/modules/accessibility.js"></script>
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
					count.push(entries[category][metric]['count']);
					highestValues.push(entries[category][metric]['highestValue']);
					middleValues.push(entries[category][metric]['middleValue']);
					lowestValues.push(entries[category][metric]['lowestValue']);
				}
				showChart('chartBox', '[Realtime Statistics] DataType: ' + dataType + ', Name: ' + name + ', Metric: '+ metric, categories, count, highestValues, middleValues, lowestValues);
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
						text: '<b>' + title + '</b>',
						align: 'left'
				},
				exporting: false,
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
						backgroundColor: 
							Highcharts.defaultOptions.legend.backgroundColor || 'rgba(255,255,255,0.25)'
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
		<label id="title">Vortex Metrics</label>
	</div>
	<div id="container">
		<div id="searchBox">
			<label>Location: </label><input type="text" id="apiLocation" />
			<input type="button" value="Search" id="searchBtn"/>
		</div>
		<div id="chartBox">
		</div>
	</div>
	<div id="foot">
		Copyright @2018-2021 Fred Feng. All Rights Reserved.
	</div>
</body>
</html>
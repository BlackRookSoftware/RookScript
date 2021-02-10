function github_api_start(data)
{
	let repourl = null;
	repourl = data.repository_url
		.replace('{owner}', REPO_OWNER)
		.replace('{repo}', REPO_NAME[0]);
	$INC(repourl + "/releases?callback=display_release_core");
}

// ================================================================================

$Q1('body').onload = function()
{
	$Q1('#link-github-core').setAttribute('href', 'https://github.com/'+REPO_OWNER+'/'+REPO_NAME[0]);
	$Q1('#link-github-release-core').setAttribute('href', 'https://github.com/'+REPO_OWNER+'/'+REPO_NAME[0]+'/releases');
	$INCCALL("https://api.github.com/", github_api_start);
	$IncludeHTML();
};

// ================================================================================

function display_release_core(response)
{
	display_release_data(response.data[0], $Q1('#releases-core'), $Q1('#release-version-core'), $Q1('#site-release-links-core'));
}

function display_release_data(release, release_section_element, release_version_element, release_links_element)
{
	let version = release.name;
	let SORTASSET = function(asset)
	{
		let filename = asset.name;
		if (filename.endsWith('.jar'))
		{
			if (filename.indexOf('-sources') >= 0)
				return 1;
			else if (filename.indexOf('-javadoc') >= 0)
				return 2;
			else
				return 0;
		}
		else
		{
			if (filename.indexOf('-src') >= 0)
				return 4;
			else if (filename.indexOf('-javadocs') >= 0)
				return 5;
			else
				return 3;
		}
	};

	let GENTITLE = function(filename) 
	{
		if (filename.endsWith('.jar'))
		{
			if (filename.indexOf('-sources') >= 0)
				return 'Download Source JAR';
			else if (filename.indexOf('-javadoc') >= 0)
				return 'Download Javadoc JAR';
			else
				return 'Download JAR';
		}
		else
		{
			if (filename.indexOf('-src') >= 0)
				return 'Download Source ZIP';
			else if (filename.indexOf('-javadocs') >= 0)
				return 'Download Javadoc ZIP';
			else
				return 'Download ZIP';
		}
	};

	release.assets = release.assets.sort((a,b) => {return SORTASSET(a) - SORTASSET(b)});

	$E(release.assets, (asset)=>{
		let linkhtml = [
			GENTITLE(asset.name),
			'<span class="w3-small">'+asset.name+'</span>',
			parseInt(asset.size / 1024) + ' KB',
		].join('<br/>');

		let link = $Element('a', {
			"href": asset.browser_download_url, 
			"class": 'w3-button w3-red w3-round-large w3-margin',
			"style": 'width:275px'
		});
		link.innerHTML = linkhtml;

		release_links_element.appendChild($Element('div', {"class":'w3-col m6 l4 w3-center'}, [link]));
	});

	release_version_element.innerHTML = version;
	$ClassRemove(release_section_element, 'site-start-hidden');
}

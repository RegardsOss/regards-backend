ALTER TABLE ${flyway:defaultSchema}.t_toponyms ADD COLUMN bounding_box text;

UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":29.3772,"lon":60.5176034},"ne":{"lat":38.4910682,"lon":74.889862}}'
             WHERE bid = 'Afghanistan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":39.6448625,"lon":19.1246095},"ne":{"lat":42.6610848,"lon":21.0574335}}'
             WHERE bid = 'Albania';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":18.968147,"lon":-8.668908},"ne":{"lat":37.2962055,"lon":11.997337}}'
             WHERE bid = 'Algeria';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-14.7608358,"lon":-171.2951296},"ne":{"lat":-10.8449746,"lon":-167.9322899}}'
             WHERE bid = 'American_Samoa';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":42.4288238,"lon":1.4135781},"ne":{"lat":42.6559357,"lon":1.7863837}}'
             WHERE bid = 'Andorra';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-18.038945,"lon":11.4609793},"ne":{"lat":-4.3880634,"lon":24.0878856}}'
             WHERE bid = 'Angola';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":18.0615454,"lon":-63.6391992},"ne":{"lat":18.7951194,"lon":-62.7125449}}'
             WHERE bid = 'Anguilla';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-85.0511287,"lon":-180},"ne":{"lat":-60,"lon":180}}'
             WHERE bid = 'Antarctica';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":16.7573901,"lon":-62.5536517},"ne":{"lat":17.929,"lon":-61.447857}}'
             WHERE bid = 'Antigua_and_Barb';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-55.1850761,"lon":-73.5600329},"ne":{"lat":-21.781168,"lon":-53.6374515}}'
             WHERE bid = 'Argentina';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":38.8404775,"lon":43.4471395},"ne":{"lat":41.300712,"lon":46.6333087}}'
             WHERE bid = 'Armenia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.1702998,"lon":-70.2809842},"ne":{"lat":12.8102998,"lon":-69.6409842}}'
             WHERE bid = 'Aruba';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-55.3228175,"lon":72.2460938},"ne":{"lat":-9.0882278,"lon":168.2249543}}'
             WHERE bid = 'Australia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":46.3722761,"lon":9.5307487},"ne":{"lat":49.0205305,"lon":17.160776}}'
             WHERE bid = 'Austria';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":38.3929551,"lon":44.7633701},"ne":{"lat":41.9502947,"lon":51.0090302}}'
             WHERE bid = 'Azerbaijan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":20.7059846,"lon":-80.7001941},"ne":{"lat":27.4734551,"lon":-72.4477521}}'
             WHERE bid = 'Bahamas,_The';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":25.535,"lon":50.2697989},"ne":{"lat":26.6872444,"lon":50.9233693}}'
             WHERE bid = 'Bahrain';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":0.18,"lon":-176.45},"ne":{"lat":0.25,"lon":-176.5}}'
             WHERE bid = 'Baker_Island';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":20.3756582,"lon":88.0075306},"ne":{"lat":26.6382534,"lon":92.6804979}}'
             WHERE bid = 'Bangladesh';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.845,"lon":-59.8562115},"ne":{"lat":13.535,"lon":-59.2147175}}'
             WHERE bid = 'Barbados';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":49.4969821,"lon":2.3889137},"ne":{"lat":51.5516667,"lon":6.408097}}'
             WHERE bid = 'Belgium';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":15.8857286,"lon":-89.2262083},"ne":{"lat":18.496001,"lon":-87.3098494}}'
             WHERE bid = 'Belize';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":6.0398696,"lon":0.776667},"ne":{"lat":12.4092447,"lon":3.843343}}'
             WHERE bid = 'Benin';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":32.0469651,"lon":-65.1232222},"ne":{"lat":32.5913693,"lon":-64.4109842}}'
             WHERE bid = 'Bermuda';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":26.702016,"lon":88.7464724},"ne":{"lat":28.246987,"lon":92.1252321}}'
             WHERE bid = 'Bhutan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-22.8982742,"lon":-69.6450073},"ne":{"lat":-9.6689438,"lon":-57.453}}'
             WHERE bid = 'Bolivia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":42.5553114,"lon":15.7287433},"ne":{"lat":45.2764135,"lon":19.6237311}}'
             WHERE bid = 'Bosnia_and_Herze';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-26.9059669,"lon":19.9986474},"ne":{"lat":-17.778137,"lon":29.375304}}'
             WHERE bid = 'Botswana';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-54.38624792106547,"lon":3.2779976554744508},"ne":{"lat":-54.45514166851047,"lon":3.4367848578044686}}'
             WHERE bid = 'Bouvet_Island';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-33.8689056,"lon":-73.9830625},"ne":{"lat":5.2842873,"lon":-28.6341164}}'
             WHERE bid = 'Brazil';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-7.6454079,"lon":71.036504},"ne":{"lat":-5.037066,"lon":72.7020157}}'
             WHERE bid = 'British_Indian_O';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":17.623468,"lon":-65.159094},"ne":{"lat":18.464984,"lon":-64.512674}}'
             WHERE bid = 'British_Virgin_I';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":4.002508,"lon":114.0758734},"ne":{"lat":5.1011857,"lon":115.3635623}}'
             WHERE bid = 'Brunei';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":41.2353929,"lon":22.3571459},"ne":{"lat":44.2167064,"lon":28.8875409}}'
             WHERE bid = 'Bulgaria';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":9.4104718,"lon":-5.5132416},"ne":{"lat":15.084,"lon":2.4089717}}'
             WHERE bid = 'Burkina_Faso';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-4.4693155,"lon":29.0007401},"ne":{"lat":-2.3096796,"lon":30.8498462}}'
             WHERE bid = 'Burundi';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":51.2575982,"lon":23.1783344},"ne":{"lat":56.17218,"lon":32.7627809}}'
             WHERE bid = 'Byelarus';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":9.4752639,"lon":102.3338282},"ne":{"lat":14.6904224,"lon":107.6276788}}'
             WHERE bid = 'Cambodia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":1.6546659,"lon":8.3822176},"ne":{"lat":13.083333,"lon":16.1921476}}'
             WHERE bid = 'Cameroon';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":41.6765556,"lon":-141.00275},"ne":{"lat":83.3362128,"lon":-52.3231981}}'
             WHERE bid = 'Canada';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":14.8031546,"lon":-25.3609478},"ne":{"lat":17.2053108,"lon":-22.6673416}}'
             WHERE bid = 'Cape_Verde';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":19.0620619,"lon":-81.6313748},"ne":{"lat":19.9573759,"lon":-79.5110954}}'
             WHERE bid = 'Cayman_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":2.2156553,"lon":14.4155426},"ne":{"lat":11.001389,"lon":27.4540764}}'
             WHERE bid = 'Central_African_';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":7.44107,"lon":13.47348},"ne":{"lat":23.4975,"lon":24}}'
             WHERE bid = 'Chad';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-56.725,"lon":-109.6795789},"ne":{"lat":-17.4983998,"lon":-66.0753474}}'
             WHERE bid = 'Chile';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":8.8383436,"lon":73.4997347},"ne":{"lat":53.5608154,"lon":134.7754563}}'
             WHERE bid = 'China';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-10.5698515,"lon":105.5336422},"ne":{"lat":-10.4123553,"lon":105.7130159}}'
             WHERE bid = 'Christmas_Island';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-12.4055983,"lon":96.612524},"ne":{"lat":-11.6213132,"lon":97.1357343}}'
             WHERE bid = 'Cocos_(Keeling)_';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-4.2316872,"lon":-82.1243666},"ne":{"lat":16.0571269,"lon":-66.8511907}}'
             WHERE bid = 'Colombia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-12.621,"lon":43.025305},"ne":{"lat":-11.165,"lon":44.7451922}}'
             WHERE bid = 'Comoros';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-5.149089,"lon":11.0048205},"ne":{"lat":3.713056,"lon":18.643611}}'
             WHERE bid = 'Congo';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-22.15807,"lon":-166.0856468},"ne":{"lat":-8.7168792,"lon":-157.1089329}}'
             WHERE bid = 'Cook_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":5.3329698,"lon":-87.2722647},"ne":{"lat":11.2195684,"lon":-82.5060208}}'
             WHERE bid = 'Costa_Rica';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":42.1765993,"lon":13.2104814},"ne":{"lat":46.555029,"lon":19.4470842}}'
             WHERE bid = 'Croatia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":19.6275294,"lon":-85.1679702},"ne":{"lat":23.4816972,"lon":-73.9190004}}'
             WHERE bid = 'Cuba';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":34.4383706,"lon":32.0227581},"ne":{"lat":35.913252,"lon":34.8553182}}'
             WHERE bid = 'Cyprus';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":48.5518083,"lon":12.0905901},"ne":{"lat":51.0557036,"lon":18.859216}}'
             WHERE bid = 'Czech_Republic';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":54.4516667,"lon":7.7153255},"ne":{"lat":57.9524297,"lon":15.5530641}}'
             WHERE bid = 'Denmark';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":10.9149547,"lon":41.7713139},"ne":{"lat":12.7923081,"lon":43.6579046}}'
             WHERE bid = 'Djibouti';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":15.0074207,"lon":-61.6869184},"ne":{"lat":15.7872222,"lon":-61.0329895}}'
             WHERE bid = 'Dominica';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":17.2701708,"lon":-72.0574706},"ne":{"lat":21.303433,"lon":-68.1101463}}'
             WHERE bid = 'Dominican_Republ';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-5.0159314,"lon":-92.2072392},"ne":{"lat":1.8835964,"lon":-75.192504}}'
             WHERE bid = 'Ecuador';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":22,"lon":24.6499112},"ne":{"lat":31.8330854,"lon":37.1153517}}'
             WHERE bid = 'Egypt';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.976046,"lon":-90.1790975},"ne":{"lat":14.4510488,"lon":-87.6351394}}'
             WHERE bid = 'El_Salvador';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.976046,"lon":-90.1790975},"ne":{"lat":14.4510488,"lon":-87.6351394}}'
             WHERE bid = 'Equatorial_Guine';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.3548219,"lon":36.4333653},"ne":{"lat":18.0709917,"lon":43.3001714}}'
             WHERE bid = 'Eritrea';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":57.5092997,"lon":21.3826069},"ne":{"lat":59.9383754,"lon":28.2100175}}'
             WHERE bid = 'Estonia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":3.397448,"lon":32.9975838},"ne":{"lat":14.8940537,"lon":47.9823797}}'
             WHERE bid = 'Ethiopia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-53.1186766,"lon":-61.7726772},"ne":{"lat":-50.7973007,"lon":-57.3662367}}'
             WHERE bid = 'Falkland_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":61.3915553,"lon":-7.6882939},"ne":{"lat":62.3942991,"lon":-6.2565525}}'
             WHERE bid = 'Faroe_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":0.827,"lon":137.2234512},"ne":{"lat":10.291,"lon":163.2364054}}'
             WHERE bid = 'Federated_States';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-21.9434274,"lon":172},"ne":{"lat":-12.2613866,"lon":-178.5}}'
             WHERE bid = 'Fiji';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":59.4541578,"lon":19.0832098},"ne":{"lat":70.0922939,"lon":31.5867071}}'
             WHERE bid = 'Finland';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":41.2632185,"lon":-5.4534286},"ne":{"lat":51.268318,"lon":9.8678344}}'
             WHERE bid = 'France';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":2.112222,"lon":-54.60278},"ne":{"lat":5.7507111,"lon":-51.6346139}}'
             WHERE bid = 'French_Guiana';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-28.0990232,"lon":-154.9360599},"ne":{"lat":-7.6592173,"lon":-134.244799}}'
             WHERE bid = 'French_Polynesia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-50.2187169,"lon":39.4138676},"ne":{"lat":-11.3139928,"lon":77.8494974}}'
             WHERE bid = 'French_Southern_';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-4.1012261,"lon":8.5002246},"ne":{"lat":2.3182171,"lon":14.539444}}'
             WHERE bid = 'Gabon';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":13.061,"lon":-17.0288254},"ne":{"lat":13.8253137,"lon":-13.797778}}'
             WHERE bid = 'Gambia,_The';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":31.60574485361819,"lon":34.56745721271432},"ne":{"lat":31.222397650139342,"lon":34.211802980624725}}'
             WHERE bid = 'Gaza_Strip';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":41.0552922,"lon":39.8844803},"ne":{"lat":43.5864294,"lon":46.7365373}}'
             WHERE bid = 'Georgia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":47.2701114,"lon":5.8663153},"ne":{"lat":55.099161,"lon":15.0419319}}'
             WHERE bid = 'Germany';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":4.5392525,"lon":-3.260786},"ne":{"lat":11.1748562,"lon":1.2732942}}'
             WHERE bid = 'Ghana';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":36.100807,"lon":-5.3941295},"ne":{"lat":36.180807,"lon":-5.3141295}}'
             WHERE bid = 'Gibraltar';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-11.51464677057173,"lon":47.37907806668858},"ne":{"lat":-11.51464677057173,"lon":47.28453806047679}}'
             WHERE bid = 'Glorioso_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":34.7006096,"lon":19.2477876},"ne":{"lat":41.7488862,"lon":29.7296986}}'
             WHERE bid = 'Greece';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":59.515387,"lon":-74.1250416},"ne":{"lat":83.875172,"lon":-10.0288759}}'
             WHERE bid = 'Greenland';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":11.786,"lon":-62.0065868},"ne":{"lat":12.5966532,"lon":-61.1732143}}'
             WHERE bid = 'Grenada';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":15.8320085,"lon":-61.809764},"ne":{"lat":16.5144664,"lon":-61.0003663}}'
             WHERE bid = 'Guadeloupe';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":13.182335,"lon":144.563426},"ne":{"lat":13.706179,"lon":145.009167}}'
             WHERE bid = 'Guam';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":13.6345804,"lon":-92.3105242},"ne":{"lat":17.8165947,"lon":-88.1755849}}'
             WHERE bid = 'Guatemala';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":49.4155331,"lon":-2.6751703},"ne":{"lat":49.5090776,"lon":-2.501814}}'
             WHERE bid = 'Guernsey';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":7.1906045,"lon":-15.5680508},"ne":{"lat":12.67563,"lon":-7.6381993}}'
             WHERE bid = 'Guinea';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":10.6514215,"lon":-16.894523},"ne":{"lat":12.6862384,"lon":-13.6348777}}'
             WHERE bid = 'Guinea-Bissau';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":1.1710017,"lon":-61.414905},"ne":{"lat":8.6038842,"lon":-56.4689543}}'
             WHERE bid = 'Guyana';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":17.9099291,"lon":-75.2384618},"ne":{"lat":20.2181368,"lon":-71.6217461}}'
             WHERE bid = 'Haiti';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-53.394741,"lon":72.2460938},"ne":{"lat":-52.7030677,"lon":74.1988754}}'
             WHERE bid = 'Heard_Island_&_M';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.9808485,"lon":-89.3568207},"ne":{"lat":17.619526,"lon":-82.1729621}}'
             WHERE bid = 'Honduras';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":0.8202254390333934,"lon":-176.62455229996525},"ne":{"lat":0.7955144648952626,"lon":-176.61068197214777}}'
             WHERE bid = 'Howland_Island';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":45.737128,"lon":16.1138867},"ne":{"lat":48.585257,"lon":22.8977094}}'
             WHERE bid = 'Hungary';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":63.0859177,"lon":-25.0135069},"ne":{"lat":67.353,"lon":-12.8046162}}'
             WHERE bid = 'Iceland';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":6.5546079,"lon":68.1113787},"ne":{"lat":35.6745457,"lon":97.395561}}'
             WHERE bid = 'India';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-11.2085669,"lon":94.7717124},"ne":{"lat":6.2744496,"lon":141.0194444}}'
             WHERE bid = 'Indonesia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":24.8465103,"lon":44.0318908},"ne":{"lat":39.7816502,"lon":63.3332704}}'
             WHERE bid = 'Iran';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":29.0585661,"lon":38.7936719},"ne":{"lat":37.380932,"lon":48.8412702}}'
             WHERE bid = 'Iraq';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":51.222,"lon":-11.0133788},"ne":{"lat":55.636,"lon":-5.6582363}}'
             WHERE bid = 'Ireland';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":29.4533796,"lon":34.2674994},"ne":{"lat":33.3356317,"lon":35.8950234}}'
             WHERE bid = 'Israel';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":35.2889616,"lon":6.6272658},"ne":{"lat":47.0921462,"lon":18.7844746}}'
             WHERE bid = 'Italy';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-8.60288021487,"lon":4.33828847902},"ne":{"lat":-2.56218950033,"lon":10.5240607772}}'
             WHERE bid = 'Ivory_Coast';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":16.5899443,"lon":-78.5782366},"ne":{"lat":18.7256394,"lon":-75.7541143}}'
             WHERE bid = 'Jamaica';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":70.6260825,"lon":-9.6848146},"ne":{"lat":81.028076,"lon":34.6891253}}'
             WHERE bid = 'Jan_Mayen';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":20.2145811,"lon":122.7141754},"ne":{"lat":45.7112046,"lon":154.205541}}'
             WHERE bid = 'Japan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-0.3808667179788756,"lon":-159.9848346714694},"ne":{"lat":45.7112046,"lon":154.205541}}'
             WHERE bid = 'Jarvis_Island';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":49.1625179,"lon":-2.254512},"ne":{"lat":49.2621288,"lon":-2.0104193}}'
             WHERE bid = 'Jersey';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":16.719016474203855,"lon":-169.54824582063927},"ne":{"lat":16.73867006222268,"lon":-169.5190017830063}}'
             WHERE bid = 'Johnston_Atoll';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":29.183401,"lon":34.8844372},"ne":{"lat":33.3750617,"lon":39.3012981}}'
             WHERE bid = 'Jordan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-17.064811194503804,"lon":42.75101308180922},"ne":{"lat":-17.044337520775485,"lon":42.69829821199883}}'
             WHERE bid = 'Juan_De_Nova_Isl';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":40.5686476,"lon":46.4932179},"ne":{"lat":55.4421701,"lon":87.3156316}}'
             WHERE bid = 'Kazakhstan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-4.8995204,"lon":33.9098987},"ne":{"lat":4.62,"lon":41.899578}}'
             WHERE bid = 'Kenya';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-7.0516717,"lon":-179.1645388},"ne":{"lat":7.9483283,"lon":-164.1645388}}'
             WHERE bid = 'Kiribati';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":28.5243622,"lon":46.5526837},"ne":{"lat":30.1038082,"lon":49.0046809}}'
             WHERE bid = 'Kuwait';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":39.1728437,"lon":69.2649523},"ne":{"lat":43.2667971,"lon":80.2295793}}'
             WHERE bid = 'Kyrgyzstan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":13.88109101,"lon":100.115987583},"ne":{"lat":22.4647531194,"lon":107.564525181}}'
             WHERE bid = 'Laos';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":55.6746505,"lon":20.6715407},"ne":{"lat":58.0855688,"lon":28.2414904}}'
             WHERE bid = 'Latvia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":33.0479858,"lon":34.8825667},"ne":{"lat":34.6923543,"lon":36.625}}'
             WHERE bid = 'Lebanon';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-30.6772773,"lon":27.0114632},"ne":{"lat":-28.570615,"lon":29.4557099}}'
             WHERE bid = 'Lesotho';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":4.1555907,"lon":-11.6080764},"ne":{"lat":8.5519861,"lon":-7.367323}}'
             WHERE bid = 'Liberia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":19.5008138,"lon":9.391081},"ne":{"lat":33.3545898,"lon":25.3770629}}'
             WHERE bid = 'Libya';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":47.0484291,"lon":9.4716736},"ne":{"lat":47.270581,"lon":9.6357143}}'
             WHERE bid = 'Liechtenstein';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":53.8967893,"lon":20.653783},"ne":{"lat":56.4504213,"lon":26.8355198}}'
             WHERE bid = 'Lithuania';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":49.4969821,"lon":4.9684415},"ne":{"lat":50.430377,"lon":6.0344254}}'
             WHERE bid = 'Luxembourg';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":22.177986013607125,"lon":113.52841820624815},"ne":{"lat":22.21738947303271,"lon":113.56394453397058}}'
             WHERE bid = 'Macau';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":40.8536596,"lon":0.4529023},"ne":{"lat":42.3735359,"lon":23.034051}}'
             WHERE bid = 'Macedonia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-25.6071002,"lon":43.2202072},"ne":{"lat":-11.9519693,"lon":50.4862553}}'
             WHERE bid = 'Madagascar';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-17.1296031,"lon":32.6703616},"ne":{"lat":-9.3683261,"lon":35.9185731}}'
             WHERE bid = 'Malawi';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-5.1076241,"lon":105.3471939},"ne":{"lat":9.8923759,"lon":120.3471939}}'
             WHERE bid = 'Malaysia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-0.9074935,"lon":72.3554187},"ne":{"lat":7.3106246,"lon":73.9700962}}'
             WHERE bid = 'Maldives';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":10.147811,"lon":-12.2402835},"ne":{"lat":25.001084,"lon":4.2673828}}'
             WHERE bid = 'Mali';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":35.6029696,"lon":13.9324226},"ne":{"lat":36.2852706,"lon":14.8267966}}'
             WHERE bid = 'Malta';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":54.0539576,"lon":-4.7946845},"ne":{"lat":54.4178705,"lon":-4.3076853}}'
             WHERE bid = 'Man,_Isle_of';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-0.5481258,"lon":163.4985095},"ne":{"lat":14.4518742,"lon":178.4985095}}'
             WHERE bid = 'Marshall_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":14.3948596,"lon":-61.2290815},"ne":{"lat":14.8787029,"lon":-60.8095833}}'
             WHERE bid = 'Martinique';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":14.7209909,"lon":-17.068081},"ne":{"lat":27.314942,"lon":-4.8333344}}'
             WHERE bid = 'Mauritania';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-20.725,"lon":56.3825151},"ne":{"lat":-10.138,"lon":63.7151319}}'
             WHERE bid = 'Mauritius';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-13.0210119,"lon":45.0183298},"ne":{"lat":-12.6365902,"lon":45.2999917}}'
             WHERE bid = 'Mayotte';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":14.3886243,"lon":-118.59919},"ne":{"lat":32.7186553,"lon":-86.493266}}'
             WHERE bid = 'Mexico';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":27.983569867765524,"lon":-82.81410378650139},"ne":{"lat":27.973649029897842,"lon":-82.81154270958145}}'
             WHERE bid = 'Midway_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":45.4674139,"lon":26.6162189},"ne":{"lat":48.4918695,"lon":30.1636756}}'
             WHERE bid = 'Moldova';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":43.7247599,"lon":7.4090279},"ne":{"lat":43.7519311,"lon":7.4398704}}'
             WHERE bid = 'Monaco';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":41.5800276,"lon":87.73762},"ne":{"lat":52.1496,"lon":119.931949}}'
             WHERE bid = 'Mongolia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":41.7495999,"lon":18.4195781},"ne":{"lat":43.5585061,"lon":20.3561641}}'
             WHERE bid = 'Montenegro';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":16.475,"lon":-62.450667},"ne":{"lat":17.0152978,"lon":-61.9353818}}'
             WHERE bid = 'Montserrat';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":21.3365321,"lon":-17.2551456},"ne":{"lat":36.0505269,"lon":-0.998429}}'
             WHERE bid = 'Morocco';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-26.9209427,"lon":30.2138197},"ne":{"lat":-10.3252149,"lon":41.0545908}}'
             WHERE bid = 'Mozambique';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":9.4399432,"lon":92.1719423},"ne":{"lat":28.547835,"lon":101.1700796}}'
             WHERE bid = 'Myanmar_(Burma)';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-28.96945,"lon":11.5280384},"ne":{"lat":-16.9634855,"lon":25.2617671}}'
             WHERE bid = 'Namibia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-0.5541334,"lon":166.9091794},"ne":{"lat":-0.5025906,"lon":166.9589235}}'
             WHERE bid = 'Nauru';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":26.3477581,"lon":80.0586226},"ne":{"lat":30.446945,"lon":88.2015257}}'
             WHERE bid = 'Nepal';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":50.7295671,"lon":1.9193492},"ne":{"lat":53.7253321,"lon":7.2274985}}'
             WHERE bid = 'Netherlands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.1544542,"lon":-68.940593},"ne":{"lat":12.1547472,"lon":-68.9403518}}'
             WHERE bid = 'Netherlands_Anti';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-23.2217509,"lon":162.6034343},"ne":{"lat":-17.6868616,"lon":167.8109827}}'
             WHERE bid = 'New_Caledonia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-52.8213687,"lon":-179.059153},"ne":{"lat":-29.0303303,"lon":179.3643594}}'
             WHERE bid = 'New_Zealand';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":10.7076565,"lon":-87.901532},"ne":{"lat":15.0331183,"lon":-82.6227023}}'
             WHERE bid = 'Nicaragua';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":11.693756,"lon":0.1689653},"ne":{"lat":23.517178,"lon":15.996667}}'
             WHERE bid = 'Niger';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":4.0690959,"lon":2.676932},"ne":{"lat":13.885645,"lon":14.678014}}'
             WHERE bid = 'Nigeria';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-19.3548665,"lon":-170.1595029},"ne":{"lat":-18.7534559,"lon":-169.5647229}}'
             WHERE bid = 'Niue';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-29.333,"lon":167.6873878},"ne":{"lat":-28.796,"lon":168.2249543}}'
             WHERE bid = 'Norfolk_Island';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":14.036565,"lon":144.813338},"ne":{"lat":20.616556,"lon":146.154418}}'
             WHERE bid = 'Northern_Mariana';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":37.5867855,"lon":124.0913902},"ne":{"lat":43.0089642,"lon":130.924647}}'
             WHERE bid = 'North_Korea';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":57.7590052,"lon":4.0875274},"ne":{"lat":71.3848787,"lon":31.7614911}}'
             WHERE bid = 'Norway';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":16.4649608,"lon":52},"ne":{"lat":26.7026737,"lon":60.054577}}'
             WHERE bid = 'Oman';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":2.748,"lon":131.0685462},"ne":{"lat":8.222,"lon":134.7714735}}'
             WHERE bid = 'Pacific_Islands_';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":23.5393916,"lon":60.872855},"ne":{"lat":37.084107,"lon":77.1203914}}'
             WHERE bid = 'Pakistan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":7.0338679,"lon":-83.0517245},"ne":{"lat":9.8701757,"lon":-77.1393779}}'
             WHERE bid = 'Panama';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-13.1816069,"lon":136.7489081},"ne":{"lat":1.8183931,"lon":151.7489081}}'
             WHERE bid = 'Papua_New_Guinea';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":16.658492196230355,"lon":112.71969134650925},"ne":{"lat":16.676123364300892,"lon":112.74055410189226}}'
             WHERE bid = 'Paracel_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-27.6063935,"lon":-62.6442036},"ne":{"lat":-19.2876472,"lon":-54.258}}'
             WHERE bid = 'Paraguay';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-20.1984472,"lon":-84.6356535},"ne":{"lat":-0.0392818,"lon":-68.6519906}}'
             WHERE bid = 'Peru';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":4.2158064,"lon":114.0952145},"ne":{"lat":21.3217806,"lon":126.8072562}}'
             WHERE bid = 'Philippines';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-25.1306736,"lon":-130.8049862},"ne":{"lat":-23.8655769,"lon":-124.717534}}'
             WHERE bid = 'Pitcairn_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":49.0020468,"lon":14.1229707},"ne":{"lat":55.0336963,"lon":24.145783}}'
             WHERE bid = 'Poland';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":29.8288021,"lon":-31.5575303},"ne":{"lat":42.1543112,"lon":-6.1891593}}'
             WHERE bid = 'Portugal';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":17.9268695,"lon":-67.271492},"ne":{"lat":18.5159789,"lon":-65.5897525}}'
             WHERE bid = 'Puerto_Rico';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":24.4707534,"lon":50.5675},"ne":{"lat":26.3830212,"lon":52.638011}}'
             WHERE bid = 'Qatar';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":24.4707534,"lon":50.5675},"ne":{"lat":26.3830212,"lon":52.638011}}'
             WHERE bid = 'Qatar';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-21.3897308,"lon":55.2164268},"ne":{"lat":-20.8717136,"lon":55.8366924}}'
             WHERE bid = 'Reunion';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":43.618682,"lon":20.2619773},"ne":{"lat":48.2653964,"lon":30.0454257}}'
             WHERE bid = 'Romania';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":41.1850968,"lon":19.6389},"ne":{"lat":82.0586232,"lon":180}}'
             WHERE bid = 'Russia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-2.8389804,"lon":28.8617546},"ne":{"lat":-1.0474083,"lon":30.8990738}}'
             WHERE bid = 'Rwanda';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":43.8937002,"lon":12.4033246},"ne":{"lat":43.992093,"lon":12.5160665}}'
             WHERE bid = 'San_Marino';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-0.013350224548574374,"lon":6.4070644897324485},"ne":{"lat":0.4391646693353266,"lon":6.808321743996032}}'
             WHERE bid = 'Sao_Tome_and_Pri';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":16.29,"lon":34.4571718},"ne":{"lat":32.1543377,"lon":55.6666851}}'
             WHERE bid = 'Saudi_Arabia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.2372838,"lon":-17.7862419},"ne":{"lat":16.6919712,"lon":-11.3458996}}'
             WHERE bid = 'Senegal';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":42.2322435,"lon":18.8142875},"ne":{"lat":46.1900524,"lon":23.006309}}'
             WHERE bid = 'Serbia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-10.4649258,"lon":45.9988759},"ne":{"lat":-3.512,"lon":56.4979396}}'
             WHERE bid = 'Seychelles';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":6.755,"lon":-13.5003389},"ne":{"lat":9.999973,"lon":-10.271683}}'
             WHERE bid = 'Sierra_Leone';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":1.1304753,"lon":103.6920359},"ne":{"lat":1.4504753,"lon":104.0120359}}'
             WHERE bid = 'Singapore';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":47.7314286,"lon":16.8331891},"ne":{"lat":49.6138162,"lon":22.56571}}'
             WHERE bid = 'Slovakia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":45.4214242,"lon":13.3754696},"ne":{"lat":46.8766816,"lon":16.5967702}}'
             WHERE bid = 'Slovenia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-13.2424298,"lon":155.3190556},"ne":{"lat":-4.81085,"lon":170.3964667}}'
             WHERE bid = 'Solomon_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-1.8031969,"lon":40.98918},"ne":{"lat":12.1889121,"lon":51.6177696}}'
             WHERE bid = 'Somalia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-47.1788335,"lon":16.3335213},"ne":{"lat":-22.1250301,"lon":38.2898954}}'
             WHERE bid = 'South_Africa';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-54.938280924373586,"lon":-38.231052349964074},"ne":{"lat":-53.94141236974046,"lon":-35.86736112811525}}'
             WHERE bid = 'South_Georgia_an';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":32.9104556,"lon":124.354847},"ne":{"lat":38.623477,"lon":132.1467806}}'
             WHERE bid = 'South_Korea';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":27.4335426,"lon":-18.3936845},"ne":{"lat":43.9933088,"lon":4.5918885}}'
             WHERE bid = 'Spain';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":8.639620302576162,"lon":111.91500187640565},"ne":{"lat":8.650392563123987,"lon":111.92548932919232}}'
             WHERE bid = 'Spratly_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":5.719,"lon":79.3959205},"ne":{"lat":10.035,"lon":82.0810141}}'
             WHERE bid = 'Sri_Lanka';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-16.23,"lon":-5.9973424},"ne":{"lat":-15.704,"lon":-5.4234153}}'
             WHERE bid = 'St._Helena';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":16.895,"lon":-63.051129},"ne":{"lat":17.6158146,"lon":-62.3303519}}'
             WHERE bid = 'St._Kitts_and_Ne';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":13.508,"lon":-61.2853867},"ne":{"lat":14.2725,"lon":-60.6669363}}'
             WHERE bid = 'St._Lucia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":46.5507173,"lon":-56.6972961},"ne":{"lat":47.365,"lon":-55.9033333}}'
             WHERE bid = 'St._Pierre_and_M';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":12.5166548,"lon":-61.6657471},"ne":{"lat":13.583,"lon":-60.9094146}}'
             WHERE bid = 'St._Vincent_and_';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":8.685278,"lon":21.8145046},"ne":{"lat":22.224918,"lon":39.0576252}}'
             WHERE bid = 'Sudan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":1.8312802,"lon":-58.070833},"ne":{"lat":6.225,"lon":-53.8433358}}'
             WHERE bid = 'Suriname';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":70.6260825,"lon":-9.6848146},"ne":{"lat":81.028076,"lon":34.6891253}}'
             WHERE bid = 'Svalbard';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":25.2156672,"lon":55.1807318},"ne":{"lat":25.217255,"lon":55.18286}}'
             WHERE bid = 'Swaziland';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":55.1331192,"lon":10.5930952},"ne":{"lat":69.0599699,"lon":24.1776819}}'
             WHERE bid = 'Sweden';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":45.817995,"lon":5.9559113},"ne":{"lat":47.8084648,"lon":10.4922941}}'
             WHERE bid = 'Switzerland';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":32.311354,"lon":35.4714427},"ne":{"lat":37.3184589,"lon":42.3745687}}'
             WHERE bid = 'Syria';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":10.374269,"lon":114.3599058},"ne":{"lat":26.4372222,"lon":122.297}}'
             WHERE bid = 'Taiwan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":36.6711153,"lon":67.3332775},"ne":{"lat":41.0450935,"lon":75.1539563}}'
             WHERE bid = 'Tajikistan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-11.761254,"lon":29.3269773},"ne":{"lat":-0.9854812,"lon":40.6584071}}'
             WHERE bid = 'Tanzania,_United';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":5.612851,"lon":97.3438072},"ne":{"lat":20.4648337,"lon":105.636812}}'
             WHERE bid = 'Thailand';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":5.926547,"lon":-0.1439746},"ne":{"lat":11.1395102,"lon":1.8087605}}'
             WHERE bid = 'Togo';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-9.6442499,"lon":-172.7213673},"ne":{"lat":-8.3328631,"lon":-170.9797586}}'
             WHERE bid = 'Tokelau';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-24.1034499,"lon":-179.3866055},"ne":{"lat":-15.3655722,"lon":-173.5295458}}'
             WHERE bid = 'Tonga';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":9.8732106,"lon":-62.083056},"ne":{"lat":11.5628372,"lon":-60.2895848}}'
             WHERE bid = 'Trinidad_and_Tob';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":30.230236,"lon":7.5219807},"ne":{"lat":37.7612052,"lon":11.8801133}}'
             WHERE bid = 'Tunisia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":35.8076804,"lon":25.6212891},"ne":{"lat":42.297,"lon":44.8176638}}'
             WHERE bid = 'Turkey';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":35.129093,"lon":52.335076},"ne":{"lat":42.7975571,"lon":66.6895177}}'
             WHERE bid = 'Turkmenistan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":20.9553418,"lon":-72.6799046},"ne":{"lat":22.1630989,"lon":-70.8643591}}'
             WHERE bid = 'Turks_and_Caicos';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-9.9939389,"lon":175.1590468},"ne":{"lat":-5.4369611,"lon":178.7344938}}'
             WHERE bid = 'Tuvalu';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-1.4823179,"lon":29.573433},"ne":{"lat":4.2340766,"lon":35.000308}}'
             WHERE bid = 'Uganda';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":44.184598,"lon":22.137059},"ne":{"lat":52.3791473,"lon":40.2275801}}'
             WHERE bid = 'Ukraine';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":22.6444,"lon":51.498},"ne":{"lat":26.2822,"lon":56.3834}}'
             WHERE bid = 'United_Arab_Emir';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":49.674,"lon":-14.015517},"ne":{"lat":61.061,"lon":2.0919117}}'
             WHERE bid = 'United_Kingdom';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":24.9493,"lon":-125.0011},"ne":{"lat":49.5904,"lon":-66.9326}}'
             WHERE bid = 'United_States';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-35.7824481,"lon":-58.4948438},"ne":{"lat":-30.0853962,"lon":-53.0755833}}'
             WHERE bid = 'Uruguay';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":37.1821164,"lon":55.9977865},"ne":{"lat":45.590118,"lon":73.1397362}}'
             WHERE bid = 'Uzbekistan';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-20.4627425,"lon":166.3355255},"ne":{"lat":-12.8713777,"lon":170.449982}}'
             WHERE bid = 'Vanuatu';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":0.647529,"lon":-73.3529632},"ne":{"lat":15.9158431,"lon":-59.5427079}}'
             WHERE bid = 'Venezuela';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":8.59975962975,"lon":102.170435826},"ne":{"lat":23.3520633001,"lon":109.33526981}}'
             WHERE bid = 'Vietnam';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":17.623468,"lon":-65.159094},"ne":{"lat":18.464984,"lon":-64.512674}}'
             WHERE bid = 'Virgin_Islands';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":19.267674375956986,"lon":166.59120624908428},"ne":{"lat":19.32092542179754,"lon":166.66260493457298}}'
             WHERE bid = 'Wake_Island';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-13.38712990474788,"lon":-176.25293895709285},"ne":{"lat":-13.183222350213157,"lon":-176.14366785627365}}'
             WHERE bid = 'Wallis_and_Futun';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":31.3534353704,"lon":34.9274084816},"ne":{"lat":32.5325106878,"lon":35.5456653175}}'
             WHERE bid = 'West_Bank';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":20.556883,"lon":-17.3494721},"ne":{"lat":27.6666834,"lon":-8.666389}}'
             WHERE bid = 'Western_Sahara';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-14.7608358,"lon":-171.2951296},"ne":{"lat":-10.8449746,"lon":-167.9322899}}'
             WHERE bid = 'Western_Samoa';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":11.9084802,"lon":41.60825},"ne":{"lat":19,"lon":54.7389375}}'
             WHERE bid = 'Yemen';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-13.866775144104807,"lon":11.909404309544044},"ne":{"lat":5.637368364214822,"lon":31.24342002632318}}'
             WHERE bid = 'Zaire';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-18.0765945,"lon":21.9993509},"ne":{"lat":-8.2712822,"lon":33.701111}}'
             WHERE bid = 'Zambia';
UPDATE ${flyway:defaultSchema}.t_toponyms
             SET bounding_box = '{"sw":{"lat":-22.4241096,"lon":25.2373},"ne":{"lat":-15.6097033,"lon":33.0683413}}'
             WHERE bid = 'Zimbabwe';





































































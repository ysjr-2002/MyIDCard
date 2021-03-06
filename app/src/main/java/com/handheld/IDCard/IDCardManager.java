package com.handheld.IDCard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.util.Log;

import com.myidcard.R;
import com.pci.pca.readcard.SerialPort;
import com.pci.pca.readcard.Tools;
import com.synjones.bluetooth.DecodeWlt;
import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.WLTService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
//import com.synjones.bluetooth.DecodeWlt;


public class IDCardManager {
	private static final String TAG = null;
//	private String cmd_samid = "0xAA 0xAA 0xAA 0x96 0x69 0x00 0x03 0x12 0xFF 0xEE";
	private String cmd_1 = "AAAAAA96690003200122";//
	private byte[] cmd_find = Tools.HexString2Bytes(cmd_1);
	private String cmd_2 = "AAAAAA96690003200221";//
	private byte[] cmd_selet = Tools.HexString2Bytes(cmd_2);
	private String cmd_3 = "AAAAAA96690003300132";//
	private byte[] cmd_read = Tools.HexString2Bytes(cmd_3);
	private String cmd_4 = "AAAAAA96690003301023";//
	private byte[] cmd_read4 = Tools.HexString2Bytes(cmd_4);
	private byte[] cmd_samid = {(byte)0xAA ,(byte)0xAA ,(byte)0xAA, (byte)0x96, (byte)0x69 ,(byte)0x00 ,(byte)0x03 ,(byte)0x12 ,(byte)0xFF ,(byte)0xEE};
	
	private int seriaPort = 14;
	private int baudrate = 115200;
	private SerialPort mSerialPort;
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	
	private Context mContext;
	public IDCardManager(Context context){
		mContext = context;
		mSerialPort = new SerialPort();
//		mSerialPort.rfid_poweron();
		mSerialPort.psam_poweron();
//		mSerialPort.power_5Von();
		try {
			mSerialPort = new SerialPort(seriaPort, baudrate, 0);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mOutputStream = mSerialPort.getOutputStream();
		mInputStream = mSerialPort.getInputStream();
	}
	public void close() {
		if (mSerialPort != null) {
//			try {
//				mOutputStream.close();
//				mInputStream.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
			try {
				mInputStream.close();
				mOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
//			mSerialPort.close(seriaPort);
//			mSerialPort.rfid_poweroff(); 
			
//			mSerialPort.setGPIOlow(92);
//			mSerialPort.power_5Voff();
			mSerialPort.psam_poweroff();
		}
	}
	public byte[] getSamID(long timeout) {
		if (mSerialPort == null) return null;
		byte[] bs = sendCmd(cmd_samid, timeout, 27);
		if (bs!=null&&checkout(bs)) {
			byte[] samid = new byte[16];
			System.arraycopy(bs, 10, samid, 0, 16);
			return samid;
		}else {
			return null;
		}
	
	}
	
	public boolean findCard(long timeout) {
		if (mSerialPort == null) return false;
		byte[] findbs = sendCmd(cmd_find, timeout/2, 15);
		if (findbs!=null) {
			byte[] selectbs = sendCmd(cmd_selet, timeout, 19);
			if (selectbs!=null) {
				return true;
			}else {
//				Log.e(TAG, "Select Card Fail!");
			}
		}else {
//			Log.e(TAG, "Find Card Fail!");
		}
		return false;
	}
	
	public IDCardModel getData(long timeout) {
		if (mSerialPort == null) return null;
			byte[] readbs = sendCmd(cmd_read, timeout, 1295);
			if (readbs!=null) {
//				Log.e("read", Tools.Bytes2HexString(readbs, 60));
				return ResolveData(readbs);
			}else {
//				Log.e(TAG, "Read Card Fail!");
			}
		return null;
	}
	
	public IDCardModel getDataFP(long timeout) {
		if (mSerialPort == null) return null;
			byte[] readbs = sendCmd(cmd_read4, timeout, 2321);
			if (readbs!=null) {
//				Log.e("read", Tools.Bytes2HexString(readbs, 60));
				return ResolveDataFP(readbs);
			}else {
//				Log.e(TAG, "Read Card Fail!");
			}
		return null;
	}
	private byte[] sendCmd(byte[] cmd,long timeout,int length) {
//		 Log.e("发指令：", Tools.Bytes2HexString(cmd, cmd.length));
		try {
			mInputStream.read(new byte[4906]);
			mOutputStream.write(cmd);
			long time = System.currentTimeMillis();
			while (System.currentTimeMillis() - time<=timeout) {
				if (mInputStream.available()>=length) {
					byte[] bs = new byte[length];
					mInputStream.read(bs);
//					Log.e("接收：", Tools.Bytes2HexString(bs,bs.length));
					return bs;
				}		
			}
			{
				length = mInputStream.available();
				byte[] bs = new byte[length];
				mInputStream.read(bs);
//				Log.e("接收（错误）：", "error:"+Tools.Bytes2HexString(bs,bs.length));
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Send Cmd", e.toString());
			return null;
		}
		
	}
	private IDCardModel ResolveData(byte[] bytes) {
		IDCardModel model = new IDCardModel();
		if (!checkout(bytes)) return null;
			byte[] photo_bs = new byte[1024];
			System.arraycopy(bytes, 270, photo_bs, 0, 1024);
//			photo_bs = getPhotoBytes(photo_bs);
			byte[] name_bs = new byte[30];
			byte[] sex_bs = new byte[2];
			byte[] nation_bs = new byte[4];
			byte[] time_bs = new byte[16];
			byte[] address_bs = new byte[70];
			byte[] id_bs = new byte[36];
			byte[] office_bs = new byte[30];
			byte[] start_bs = new byte[16];
			byte[] stop_bs = new byte[16];
			byte[] newaddress_bs = new byte[36];

			System.arraycopy(bytes, 14, name_bs, 0, 30);
			System.arraycopy(bytes, 44, sex_bs, 0, 2);
			System.arraycopy(bytes, 46, nation_bs, 0, 4);
			System.arraycopy(bytes, 50, time_bs, 0, 16);
			System.arraycopy(bytes, 66, address_bs, 0, 70);
			System.arraycopy(bytes, 136, id_bs, 0, 36);
			System.arraycopy(bytes, 172, office_bs, 0, 30);
			System.arraycopy(bytes, 202, start_bs, 0, 16);
			System.arraycopy(bytes, 218, stop_bs, 0, 16);
			System.arraycopy(bytes, 234, newaddress_bs, 0, 36);

			//Log.e("name", Tools.Bytes2HexString(name_bs, name_bs.length));
			//Log.e("sex",
//					Tools.Bytes2HexString(sex_bs, 2)
//							+ "::"
//							+ Tools.Bytes2HexString("1".getBytes(),
//									"1".getBytes().length));
			//Log.e("nation", Tools.Bytes2HexString(nation_bs, 4));
			name_bs = getDataBytes(name_bs);
			sex_bs = getDataBytes(sex_bs);
			nation_bs = getDataBytes(nation_bs);
			time_bs = getDataBytes(time_bs);
			address_bs = getDataBytes(address_bs);
			id_bs = getDataBytes(id_bs);
			office_bs = getDataBytes(office_bs);
			start_bs = getDataBytes(start_bs);
			stop_bs = getDataBytes(stop_bs);
			newaddress_bs = getDataBytes(newaddress_bs);

			//Log.e("name", Tools.Bytes2HexString(name_bs, name_bs.length));
			//Log.e("sex",
//					Tools.Bytes2HexString(sex_bs, 2)
//							+ "::"
//							+ Tools.Bytes2HexString("1".getBytes(),
//									"1".getBytes().length));
			//Log.e("nation", Tools.Bytes2HexString(nation_bs, 4));
			try {
				String name = new String(name_bs, "UCS-2");
				String sex = new String(sex_bs, "UCS-2");
				sex = getSex(sex);
				String nation = new String(nation_bs, "UCS-2");
				nation = getNation(nation);
				String time = new String(time_bs, "UCS-2");
				String address = new String(address_bs, "UCS-2");
				String id = new String(id_bs, "UCS-2");
				String office = new String(office_bs, "UCS-2");
				String start = new String(start_bs, "UCS-2");
				String stop = new String(stop_bs, "UCS-2");
				String newaddress = new String(newaddress_bs, "UCS-2");

				model.setAddress(address);
				model.setBeginTime(start);
				model.setYear(time.substring(0, 4));
				model.setMonth(time.substring(4, 6));
				model.setDay(time.substring(6, 8));
				model.setEndTime(stop);
				model.setIDCardNumber(id);
				model.setName(name);
				model.setNation(nation);
				model.setPhotoBitmap(getBitmap2(photo_bs));
//				model.setPhotoBitmap(getBitmap(photo_bs));
				model.setOffice(office);
				model.setOtherData(newaddress);
				model.setSex(sex);
				return model;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		
	}
	private IDCardModel ResolveDataFP(byte[] bytes) {
		IDCardModel model = new IDCardModel();
		if (!checkout(bytes)) return null;
			byte[] photo_bs = new byte[1024];
			byte[] fp_bs1 = new byte[512];
			byte[] fp_bs2 = new byte[512];
			
			System.arraycopy(bytes, 272, photo_bs, 0, 1024);
			System.arraycopy(bytes, 1296, fp_bs1, 0, 512);
			System.arraycopy(bytes, 1808, fp_bs2, 0, 512);
			
//			photo_bs = getPhotoBytes(photo_bs);
			byte[] name_bs = new byte[30];
			byte[] sex_bs = new byte[2];
			byte[] nation_bs = new byte[4];
			byte[] time_bs = new byte[16];
			byte[] address_bs = new byte[70];
			byte[] id_bs = new byte[36];
			byte[] office_bs = new byte[30];
			byte[] start_bs = new byte[16];
			byte[] stop_bs = new byte[16];
			byte[] newaddress_bs = new byte[36];

			System.arraycopy(bytes, 16, name_bs, 0, 30);
			System.arraycopy(bytes, 46, sex_bs, 0, 2);
			System.arraycopy(bytes, 48, nation_bs, 0, 4);
			System.arraycopy(bytes, 52, time_bs, 0, 16);
			System.arraycopy(bytes, 68, address_bs, 0, 70);
			System.arraycopy(bytes, 138, id_bs, 0, 36);
			System.arraycopy(bytes, 174, office_bs, 0, 30);
			System.arraycopy(bytes, 204, start_bs, 0, 16);
			System.arraycopy(bytes, 220, stop_bs, 0, 16);
			System.arraycopy(bytes, 236, newaddress_bs, 0, 36);

			//Log.e("name", Tools.Bytes2HexString(name_bs, name_bs.length));
			//Log.e("sex",
//					Tools.Bytes2HexString(sex_bs, 2)
//							+ "::"
//							+ Tools.Bytes2HexString("1".getBytes(),
//									"1".getBytes().length));
			//Log.e("nation", Tools.Bytes2HexString(nation_bs, 4));
			name_bs = getDataBytes(name_bs);
			sex_bs = getDataBytes(sex_bs);
			nation_bs = getDataBytes(nation_bs);
			time_bs = getDataBytes(time_bs);
			address_bs = getDataBytes(address_bs);
			id_bs = getDataBytes(id_bs);
			office_bs = getDataBytes(office_bs);
			start_bs = getDataBytes(start_bs);
			stop_bs = getDataBytes(stop_bs);
			newaddress_bs = getDataBytes(newaddress_bs);

			//Log.e("name", Tools.Bytes2HexString(name_bs, name_bs.length));
			//Log.e("sex",
//					Tools.Bytes2HexString(sex_bs, 2)
//							+ "::"
//							+ Tools.Bytes2HexString("1".getBytes(),
//									"1".getBytes().length));
			//Log.e("nation", Tools.Bytes2HexString(nation_bs, 4));
			
//			AAAAAA9669050800009001000400     E1806761200020002000200020002000200020002000200020002000200032003000310031003900380039003000
//			AAAAAA9669090A000090010004000400 E180676120002000200020002000200020002000200020002000200020003200300031003100390038003900
//			AAAAAA9669090A000090010004000400 577FE89048512000200020002000200020002000200020002000200020003100300038003100390039003000

			try {
				String name = new String(name_bs, "UCS-2");
				String sex = new String(sex_bs, "UCS-2");
				sex = getSex(sex);
				String nation = new String(nation_bs, "UCS-2");
				nation = getNation(nation);
				String time = new String(time_bs, "UCS-2");
				String address = new String(address_bs, "UCS-2");
				String id = new String(id_bs, "UCS-2");
				String office = new String(office_bs, "UCS-2");
				String start = new String(start_bs, "UCS-2");
				String stop = new String(stop_bs, "UCS-2");
				String newaddress = new String(newaddress_bs, "UCS-2");

				model.setAddress(address);
				model.setBeginTime(start);
				model.setYear(time.substring(0, 4));
				model.setMonth(time.substring(4, 6));
				model.setDay(time.substring(6, 8));
				model.setEndTime(stop);
				model.setIDCardNumber(id);
				model.setName(name);
				model.setNation(nation);
				
//				byte[] testbs = Tools.HexString2Bytes("574C66007E00320000FF85195151513E710DD564F335902D039CA49073F9363BDF508BB430C833907D512589D9DE771CD3C0F0CE86B7F5417A25C175165B633A96B7006B347DE5B5C02FE5F7589CB6904F0EBEF56F11AEDA5251515A3E849C7C7FCCEC794DEEE56A76EAD1711ACAAABE430B1A90EEF131F5233EC64E25A2EA17AFC694FAFC0447662C31D765182E172FE199E688AF30BEB53208A18466603C86B51A0A35D6AB0067A74F7B8E9E3F44D74869F81867188DE7A1171E28BA94FC0232C5AB751643A9EB89725A36EF0DB660F3DD3D73DA350E57D9394C5AF8A8FA7FE91E3615318758391A6C233B8996D12B9A2E14BFAA3438ABBF51832ADE38CB6FBF293A0068C2CBF883691A35BBDEFF35862EF18FF7A1152C3EADA60EECB83EAC43FD3D92059FBA6CF7396A3AF385DF78E2775331CB81FD4405287293D8E89B58CEA161777327F5D0CB376798C351D4F18BB406AA4321AD4819F34157BD6224829398EFAC0890C1E6450F498814C99B94981081DF7AAF00E540634E5D8455B9B8BDCB6F8F61CC8E275D030D6EB8EDD74BAE515658324D6FB898C50D674F74B7ED3488A1910A97A971CF922FC4D2929686500616FB96953C4E2DD469A3C02CF365DF051945ECE2ADCC7CBA8B8774E0A9120353CB6ED094DFD7FDA950A8D308E778BF6A1F623AB3FAAA12CCE6DE8287875F7119253A42013F44EE638A86F8DA1AAD55E8C44E6C6B18706A6E02EE9FCA20D3EFF016414D703665E63CF1BB96309D11C225FE616E7ACEC1250363E99419FCB08AA97CFBB3441EDD09A56AEDAAF1A18B8DCFC6D848BA6119005FE166BB179DFE0E5E22819099964C2A856922B861C45C0EB0FD3068B4A6D923C5BDB050D6D3193F3834A24FD40716FA017929FFCD5FFC5CC4207CEE63E80F2E3005CE8100A7091E6C8FB7873A84C89B49CB9A1A542C156019354741242B75CBD7531A93F0836A505C7F319861DB91BC989151F57A2A60A88B2C33FDC7E9BA23FC32C7693C64B7CF017892CA9C39BCB445F8499EF5FB55260CA1B86E482BA4999E47F5CF99909DA464E642B4104D6C86BACAE6D0EFA6A5BC9D1E57441F82FC528FDFA10AFA3CED711D3C4D27AD936617DCB69D3EBC373AE16B16034C6BD22CAF0A7632D8CE0956603C998D1C610C387D9D673A1196369595AF3439F20182ECADA61F1639AF8C634748D585D96DD4666281A087A117A03C64D0E6F55A3ECF594666FD66B696386636F10CE81C5AB4FF8A974543ADF4997A887B6F1BA21FA8DF6E7EE294AC28B86D6B1A3EED4461378B613C04BC9751FDADA66944D0A958ECEE83216F6A83E917E11D51034A933F5A3EA45C30AB2F0222CC6839D8725A5ABC7FF4A96912AF6B190F6A9E5BEB8FF845996BC5F24E9146DAD4A1ADE63C849FF428F1E8BAFBA04F0B46F0A77F390CFEF81389B5B9B1E9BBB473");
////				byte[] testbs = Tools.HexString2Bytes("574C66007E00320000FF85185151513E710DD564F325F89A7237A3870966105EC2BDF5FB4BAC2E581930D3E68C0D13347396840704E862BD0CF011EC65C40A20CE9F06AC14E123A9625DB365181F2122B7BE41FD2635D1AEDB5251515A3EAE5123523FFE632275E449E7C15EAD11FED664E0A6793711A0C892B5574E99854DA89D646C043AEBAF968567C4B29D6D9058917BA308A1A208945AD4741371B4B1BC51F74F77B27365CFD33069510610D204D9589BDDFDA349E85772E68C595917826F7E058EAF2ABCBE8885A7E09C43F9B90CDE3F00D32C5829E1F206A8E65841D667A3958455EC2E8825D67960D14E0402E1B28085319A7F66A1C283727E62740219F307686B8D1AF285191A02E78DD428C9670D3738DA75E76AC00E41CCB2D017DF4B7E714A23B11156BBDA994E60C037D667862A8B14B2EA1C0211064245C36046F587629BCBFFA7BFD36716107912D130E24D54E8B5AC9588583BB50D8B907535BDF5002EE9CE6CD086F6598DE43C9B6380EA48B289662FE38FAE518FB23187A963B9C8D7630E79DE49E0B784DF3A24C2968186793A32962C54368404C58E925C9B41131E07B890D4ADA1E8B31311EF4F49D65F2E6522EF129500C48A1D8E24142197D38474467263117D81C8A75C07319AAA47A2C8987FEBC08A00D8B216637C4597D16A0D331AE78C87D9FEDCABA46958E08D5CF670BB52220F02F708B9592A6BEE47A17D5ED32B7BE6463E42D8FF797104FCAAA6AC148DCE329ADD7E7F98900281E2118AF42EC4C61A09CA0D4C6B86DC5027D879E4C78B943C009C476AB42717128C89FD8F97130B388C835FAD36AD2A619F7CAD79EC19908096CF3076E720723859F5B9D2410855423A2062028EDBBCB663622044508F89B813FE5572B7BFEBB7FC9B2F43A57000A695DB5730DFB4FCD920F81279CCEE6234FBD9EE222E4D0FB2AC12346C41E8F87D9F7FEBB7CA4A81AA257562B0B355EB2CBBDDE880EC297E3CB5B0E503F05326819FA8AF6607CBD6BCE68B18EFD3A819CCF50F72CF5B7A82E1A1BB42BB548522D0EE8B349752AAF05EB92236A97190391B8D95D85F6D3305524A12EDB09EC2A386AFB1AB00AC7A256A7A56E4F28EAF3776882579A750DB259031B1B497E8294886D6B20E00A8DC6C81B48837133C1844FB268C42503B274D2DA797BC2F18D4599C237AEB1B4C1BF1579A94887CE02358CC9BDC81705664A26F805A3E9BA6E23B800407EEBBE6C0319A1B00D5923639DC49C722A575CC815375124A04EC8650252830A07CBBDC4F3751D5CEB8208CDF076990B96C2EA70D22BF9F64EF0FDA75931B80F86821D0BF7F9E3A47FE5A3E04151953F7127BEBC13910A3B34DD5D6B8F12DD6F26F91AC476EFAB44075B89E85595DBBC27605ECBA883F9CE587FADB86FB871F3D3CAF298DAB8ABF42742E3CE0ED448FBF3B1041");
////				574C66007E00320000FF85195151513E710DD564F335902D039CA49073F9363BDF508BB430C833907D512589D9DE771CD3C0F0CE86B7F5417A25C175165B633A96B7006B347DE5B5C02FE5F7589CB6904F0EBEF56F11AEDA5251515A3E849C7C7FCCEC794DEEE56A76EAD1711ACAAABE430B1A90EEF131F5233EC64E25A2EA17AFC694FAFC0447662C31D765182E172FE199E688AF30BEB53208A18466603C86B51A0A35D6AB0067A74F7B8E9E3F44D74869F81867188DE7A1171E28BA94FC0232C5AB751643A9EB89725A36EF0DB660F3DD3D73DA350E57D9394C5AF8A8FA7FE91E3615318758391A6C233B8996D12B9A2E14BFAA3438ABBF51832ADE38CB6FBF293A0068C2CBF883691A35BBDEFF35862EF18FF7A1152C3EADA60EECB83EAC43FD3D92059FBA6CF7396A3AF385DF78E2775331CB81FD4405287293D8E89B58CEA161777327F5D0CB376798C351D4F18BB406AA4321AD4819F34157BD6224829398EFAC0890C1E6450F498814C99B94981081DF7AAF00E540634E5D8455B9B8BDCB6F8F61CC8E275D030D6EB8EDD74BAE515658324D6FB898C50D674F74B7ED3488A1910A97A971CF922FC4D2929686500616FB96953C4E2DD469A3C02CF365DF051945ECE2ADCC7CBA8B8774E0A9120353CB6ED094DFD7FDA950A8D308E778BF6A1F623AB3FAAA12CCE6DE8287875F7119253A42013F44EE638A86F8DA1AAD55E8C44E6C6B18706A6E02EE9FCA20D3EFF016414D703665E63CF1BB96309D11C225FE616E7ACEC1250363E99419FCB08AA97CFBB3441EDD09A56AEDAAF1A18B8DCFC6D848BA6119005FE166BB179DFE0E5E22819099964C2A856922B861C45C0EB0FD3068B4A6D923C5BDB050D6D3193F3834A24FD40716FA017929FFCD5FFC5CC4207CEE63E80F2E3005CE8100A7091E6C8FB7873A84C89B49CB9A1A542C156019354741242B75CBD7531A93F0836A505C7F319861DB91BC989151F57A2A60A88B2C33FDC7E9BA23FC32C7693C64B7CF017892CA9C39BCB445F8499EF5FB55260CA1B86E482BA4999E47F5CF99909DA464E642B4104D6C86BACAE6D0EFA6A5BC9D1E57441F82FC528FDFA10AFA3CED711D3C4D27AD936617DCB69D3EBC373AE16B16034C6BD22CAF0A7632D8CE0956603C998D1C610C387D9D673A1196369595AF3439F20182ECADA61F1639AF8C634748D585D96DD4666281A087A117A03C64D0E6F55A3ECF594666FD66B696386636F10CE81C5AB4FF8A974543ADF4997A887B6F1BA21FA8DF6E7EE294AC28B86D6B1A3EED4461378B613C04BC9751FDADA66944D0A958ECEE83216F6A83E917E11D51034A933F5A3EA45C30AB2F0222CC6839D8725A5ABC7FF4A96912AF6B190F6A9E5BEB8FF845996BC5F24E9146DAD4A1ADE63C849FF428F1E8BAFBA04F0B46F0A77F390CFEF81389B5B9B1E9BBB473
//				Log.e("photo_bs", Tools.Bytes2HexString(photo_bs, photo_bs.length));
//				Log.e("error"+testbs.length, "574C66007E00320000FF85195151513E710DD564F335902D039CA49073F9363BDF508BB430C833907D512589D9DE771CD3C0F0CE86B7F5417A25C175165B633A96B7006B347DE5B5C02FE5F7589CB6904F0EBEF56F11AEDA5251515A3E849C7C7FCCEC794DEEE56A76EAD1711ACAAABE430B1A90EEF131F5233EC64E25A2EA17AFC694FAFC0447662C31D765182E172FE199E688AF30BEB53208A18466603C86B51A0A35D6AB0067A74F7B8E9E3F44D74869F81867188DE7A1171E28BA94FC0232C5AB751643A9EB89725A36EF0DB660F3DD3D73DA350E57D9394C5AF8A8FA7FE91E3615318758391A6C233B8996D12B9A2E14BFAA3438ABBF51832ADE38CB6FBF293A0068C2CBF883691A35BBDEFF35862EF18FF7A1152C3EADA60EECB83EAC43FD3D92059FBA6CF7396A3AF385DF78E2775331CB81FD4405287293D8E89B58CEA161777327F5D0CB376798C351D4F18BB406AA4321AD4819F34157BD6224829398EFAC0890C1E6450F498814C99B94981081DF7AAF00E540634E5D8455B9B8BDCB6F8F61CC8E275D030D6EB8EDD74BAE515658324D6FB898C50D674F74B7ED3488A1910A97A971CF922FC4D2929686500616FB96953C4E2DD469A3C02CF365DF051945ECE2ADCC7CBA8B8774E0A9120353CB6ED094DFD7FDA950A8D308E778BF6A1F623AB3FAAA12CCE6DE8287875F7119253A42013F44EE638A86F8DA1AAD55E8C44E6C6B18706A6E02EE9FCA20D3EFF016414D703665E63CF1BB96309D11C225FE616E7ACEC1250363E99419FCB08AA97CFBB3441EDD09A56AEDAAF1A18B8DCFC6D848BA6119005FE166BB179DFE0E5E22819099964C2A856922B861C45C0EB0FD3068B4A6D923C5BDB050D6D3193F3834A24FD40716FA017929FFCD5FFC5CC4207CEE63E80F2E3005CE8100A7091E6C8FB7873A84C89B49CB9A1A542C156019354741242B75CBD7531A93F0836A505C7F319861DB91BC989151F57A2A60A88B2C33FDC7E9BA23FC32C7693C64B7CF017892CA9C39BCB445F8499EF5FB55260CA1B86E482BA4999E47F5CF99909DA464E642B4104D6C86BACAE6D0EFA6A5BC9D1E57441F82FC528FDFA10AFA3CED711D3C4D27AD936617DCB69D3EBC373AE16B16034C6BD22CAF0A7632D8CE0956603C998D1C610C387D9D673A1196369595AF3439F20182ECADA61F1639AF8C634748D585D96DD4666281A087A117A03C64D0E6F55A3ECF594666FD66B696386636F10CE81C5AB4FF8A974543ADF4997A887B6F1BA21FA8DF6E7EE294AC28B86D6B1A3EED4461378B613C04BC9751FDADA66944D0A958ECEE83216F6A83E917E11D51034A933F5A3EA45C30AB2F0222CC6839D8725A5ABC7FF4A96912AF6B190F6A9E5BEB8FF845996BC5F24E9146DAD4A1ADE63C849FF428F1E8BAFBA04F0B46F0A77F390CFEF81389B5B9B1E9BBB4734E670F5C1C5A200020002000200020002000200020002000200020002000");
//							  Log.e("error", "574C66007E00320000FF85185151513E710DD564F325F89A7237A3870966105EC2BDF5FB4BAC2E581930D3E68C0D13347396840704E862BD0CF011EC65C40A20CE9F06AC14E123A9625DB365181F2122B7BE41FD2635D1AEDB5251515A3EAE5123523FFE632275E449E7C15EAD11FED664E0A6793711A0C892B5574E99854DA89D646C043AEBAF968567C4B29D6D9058917BA308A1A208945AD4741371B4B1BC51F74F77B27365CFD33069510610D204D9589BDDFDA349E85772E68C595917826F7E058EAF2ABCBE8885A7E09C43F9B90CDE3F00D32C5829E1F206A8E65841D667A3958455EC2E8825D67960D14E0402E1B28085319A7F66A1C283727E62740219F307686B8D1AF285191A02E78DD428C9670D3738DA75E76AC00E41CCB2D017DF4B7E714A23B11156BBDA994E60C037D667862A8B14B2EA1C0211064245C36046F587629BCBFFA7BFD36716107912D130E24D54E8B5AC9588583BB50D8B907535BDF5002EE9CE6CD086F6598DE43C9B6380EA48B289662FE38FAE518FB23187A963B9C8D7630E79DE49E0B784DF3A24C2968186793A32962C54368404C58E925C9B41131E07B890D4ADA1E8B31311EF4F49D65F2E6522EF129500C48A1D8E24142197D38474467263117D81C8A75C07319AAA47A2C8987FEBC08A00D8B216637C4597D16A0D331AE78C87D9FEDCABA46958E08D5CF670BB52220F02F708B9592A6BEE47A17D5ED32B7BE6463E42D8FF797104FCAAA6AC148DCE329ADD7E7F98900281E2118AF42EC4C61A09CA0D4C6B86DC5027D879E4C78B943C009C476AB42717128C89FD8F97130B388C835FAD36AD2A619F7CAD79EC19908096CF3076E720723859F5B9D2410855423A2062028EDBBCB663622044508F89B813FE5572B7BFEBB7FC9B2F43A57000A695DB5730DFB4FCD920F81279CCEE6234FBD9EE222E4D0FB2AC12346C41E8F87D9F7FEBB7CA4A81AA257562B0B355EB2CBBDDE880EC297E3CB5B0E503F05326819FA8AF6607CBD6BCE68B18EFD3A819CCF50F72CF5B7A82E1A1BB42BB548522D0EE8B349752AAF05EB92236A97190391B8D95D85F6D3305524A12EDB09EC2A386AFB1AB00AC7A256A7A56E4F28EAF3776882579A750DB259031B1B497E8294886D6B20E00A8DC6C81B48837133C1844FB268C42503B274D2DA797BC2F18D4599C237AEB1B4C1BF1579A94887CE02358CC9BDC81705664A26F805A3E9BA6E23B800407EEBBE6C0319A1B00D5923639DC49C722A575CC815375124A04EC8650252830A07CBBDC4F3751D5CEB8208CDF076990B96C2EA70D22BF9F64EF0FDA75931B80F86821D0BF7F9E3A47FE5A3E04151953F7127BEBC13910A3B34DD5D6B8F12DD6F26F91AC476EFAB44075B89E85595DBBC27605ECBA883F9CE587FADB86FB871F3D3CAF298DAB8ABF42742E3CE0ED448FBF3B1041");
//				Log.e("error", "574C66007E00320000FF851A5151513E710DD564F3444880918E6BA99D040EC79299EA6E1954CD556A73C31BDFD5E4BDA6D098410C2EA131B555304DF529AB19F59B78268D8A86B0C0600FDEF52D7578961548994BE9D09D51AED95251515A3E80F32DF2CB962EC8D390ECF1E3C00689052EEC51207675275967B7853F371D0DC808CBB2A4C14E9CDFB6BC5B5396C6A46D5AA4DCB5A242CC19247AF0AFFA5F6AE84D8030075753AF7B929BF5D4D9940FA9F1F9EA975229EDD5F076FE72CEABCCB743D3705946F9B787B4445CF34FA33837BA363EC42548915583F1D41C78E6A8296A1B0C648B5C2F4BC385A13C1818F5555B2DC9B1B0C22A7F9E16D651F23C8B94C3AC897C41E376420B7B6A18C5D28E272D3CAF33C6B72C9177E24AEBFA4B48C9BF92BFD289CABA7970B3224BCE034EC0426340E02284FBEF0C8280CA7AFC8C4D8D11A2411AD53BA6F0FAE7181ADB17B2A832FBE9B1CA0B5819A93F85BE4A759CB9131936694A46832B739D84C4ABCCFC050E49BEDFD9C9B489A3178E6411812E3ABB930329FD863EA462885F33C1638E42F3DE41701CE293A74A367334A1BDEA92FC4BFA459DF1E0295988C3D0AB394B0420A021CC1A5700310F6FB71F467A8586B86F1AFF572CFCE3CAEDB1470D7586F7BF07C26D93AE51909502ABA4F609C89202821DDC12895CFAAA6036DA2B70A1A06C8920013D3993468C0237EBA43E59655BD86E4D6667F8786108B6A24AB61062D561FC3A09A91806A3AF59C4789A9F628A606C060A8AB79473031D1DA5733BF9A5799E3006EF889AF561EEB219057F50BD923D370CEF90F47B88D4C8FE02819F5BF47677DCE12055F6387CC403BA3CCC052A8A2AA19B71BDA5E774268304EB1B4B98AD9C0CE51C5C1F8877798F6431515392C064B073358952DC00B5019CD37B063A19D360AF0185CAA667808FBFBF3F5B2A5927A7641B3F4AB11F1BE1E5BCA9985666B36C5FFEE5668F62006CCFE614C9CBF8A4FEF134E8D5CA733DF2C20D5A4CAA132EC6502B70BDE1D20464F3D7D63A768DACA3E1747D00C80F637E1FABB909C3554426878C25C357AF2368C6CE0F25C7F0B6ED1395384B08B2E6047AAAA584400E4A8BF3E0C373D76B484964F4D0648972E4ACE383E330A6FF19F22E4FF3B2E33B692F90739DB26E3DB38B79157058F74F86CE0F56B821035B371A46CA3079920844B3C0454F3165DB2A547B0D11A4CF6A5A3EA74F5DC555849A67EEB40721026A033CF8DC8DEAEAD2AA4AE2330FD65F434151A0AB333047B7800B8C0AF5764AB9EEC56320D612E4014BA04CC62A6DC9E5388746089C04A595F672CE1E3BBB14BA59375A3EA712DF849DCBCF752487A3DBA75E70AE5167158CDED1BBF66454E8F2D57DE052EDDCF46155911FEC02F713C9CA74435525258BE362A3CA2D659D64FFD7CCC5114E3FDAB03AF85D");
				model.setPhotoBitmap(getBitmap2(photo_bs));
//				model.setPhotoBitmap(getBitmap(photo_bs));
				model.setOffice(office);
				model.setOtherData(newaddress);
				model.setSex(sex);
				model.setFP1(fp_bs1);
				model.setFP2(fp_bs2);
				return model;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		
	}
	private Bitmap getBitmap(byte[] photo_bs) {
		try {
			photo_bs = decode(photo_bs);
		} catch (RemoteException e1) {
			e1.printStackTrace();
			Log.e("IDCard",e1.toString());
		}
		File file = new File(mContext.getFileStreamPath("photo.bmp")
				.getAbsolutePath());
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inPreferredConfig = Bitmap.Config.RGB_565;
			opts.inPurgeable = true;
			opts.inInputShareable = true;
			Bitmap bitmap = BitmapFactory.decodeFileDescriptor(
					fileInputStream.getFD(), null, opts);
			fileInputStream.close();
			return bitmap;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e("IDCard",e.toString());
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("IDCard",e.toString());
			return null;
		}
	}

	/**
	 * 校验
	 * 
	 * @param bytes
	 * @return
	 */
	private boolean checkout(byte[] bytes) {
		byte[] bs = new byte[bytes.length - 6];
		System.arraycopy(bytes, 5, bs, 0, bs.length);
		byte b = 0x00;
		for (int i = 0; i < bs.length; i++) {
			if (i == 0) {
				b = bs[0];
			} else {
				b = (byte) (b ^ bs[i]);
			}
		}
		if (b == bytes[bytes.length - 1]) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 解析民族：
	 * 
	 * @param nation
	 * @return
	 */
	private String getNation(String nation) {
		String NATION = "";
		switch (Integer.valueOf(nation)) {
		case 1:
			NATION = mContext.getString(R.string.case1);
			break;
		case 2:
			NATION = mContext.getString(R.string.case2);
			break;
		case 3:
			NATION = mContext.getString(R.string.case3);
			break;
		case 4:
			NATION = mContext.getString(R.string.case4);
			break;
		case 5:
			NATION = mContext.getString(R.string.case5);
			break;
		case 6:
			NATION = mContext.getString(R.string.case6);
			break;
		case 7:
			NATION = mContext.getString(R.string.case7);
			break;
		case 8:
			NATION = mContext.getString(R.string.case8);
			break;
		case 9:
			NATION = mContext.getString(R.string.case9);
			break;
		case 10:
			NATION = mContext.getString(R.string.case10);
			break;
		case 11:
			NATION = mContext.getString(R.string.case11);
			break;
		case 12:
			NATION = mContext.getString(R.string.case12);
			break;
		case 13:
			NATION = mContext.getString(R.string.case13);
			break;
		case 14:
			NATION = mContext.getString(R.string.case14);
			break;
		case 15:
			NATION = mContext.getString(R.string.case15);
			break;
		case 16:
			NATION = mContext.getString(R.string.case16);
			break;
		case 17:
			NATION = mContext.getString(R.string.case17);
			break;
		case 18:
			NATION = mContext.getString(R.string.case18);
			break;
		case 19:
			NATION = mContext.getString(R.string.case19);
			break;
		case 20:
			NATION = mContext.getString(R.string.case20);
			break;
		case 21:
			NATION = mContext.getString(R.string.case21);
			break;
		case 22:
			NATION = mContext.getString(R.string.case22);
			break;
		case 23:
			NATION = mContext.getString(R.string.case23);
			break;
		case 24:
			NATION = mContext.getString(R.string.case24);
			break;
		case 25:
			NATION = mContext.getString(R.string.case25);
			break;
		case 26:
			NATION = mContext.getString(R.string.case26);
			break;
		case 27:
			NATION = mContext.getString(R.string.case27);
			break;
		case 28:
			NATION = mContext.getString(R.string.case28);
			break;
		case 29:
			NATION = mContext.getString(R.string.case29);
			break;
		case 30:
			NATION = mContext.getString(R.string.case30);
			break;
		case 31:
			NATION = mContext.getString(R.string.case31);
			break;
		case 32:
			NATION = mContext.getString(R.string.case32);
			break;
		case 33:
			NATION = mContext.getString(R.string.case33);
			break;
		case 34:
			NATION = mContext.getString(R.string.case34);
			break;
		case 35:
			NATION = mContext.getString(R.string.case35);
			break;
		case 36:
			NATION = mContext.getString(R.string.case36);
			break;
		case 37:
			NATION = mContext.getString(R.string.case37);
			break;
		case 38:
			NATION = mContext.getString(R.string.case38);
			break;
		case 39:
			NATION = mContext.getString(R.string.case39);
			break;
		case 40:
			NATION = mContext.getString(R.string.case40);
			break;
		case 41:
			NATION = mContext.getString(R.string.case41);
			break;
		case 42:
			NATION = mContext.getString(R.string.case42);
			break;
		case 43:
			NATION = mContext.getString(R.string.case43);
			break;
		case 44:
			NATION = mContext.getString(R.string.case44);
			break;
		case 45:
			NATION = mContext.getString(R.string.case45);
			break;
		case 46:
			NATION = mContext.getString(R.string.case47);
			break;
		case 47:
			NATION = mContext.getString(R.string.case47);
			break;
		case 48:
			NATION = mContext.getString(R.string.case48);
			break;
		case 49:
			NATION = mContext.getString(R.string.case49);
			break;
		case 50:
			NATION = mContext.getString(R.string.case50);
			break;
		case 51:
			NATION = mContext.getString(R.string.case51);
			break;
		case 52:
			NATION = mContext.getString(R.string.case52);
			break;
		case 53:
			NATION = mContext.getString(R.string.case53);
			break;
		case 54:
			NATION = mContext.getString(R.string.case54);
			break;
		case 55:
			NATION = mContext.getString(R.string.case55);
			break;
		case 56:
			NATION = mContext.getString(R.string.case56);
			break;
		default:
			NATION = mContext.getString(R.string.defaultminzu);
			break;
		}
		return NATION;
	}

	/**
	 * 解析性别：
	 * 
	 * @param sex
	 * @return
	 */
	private String getSex(String sex) {
		String SEX = "";
		if (Integer.valueOf(sex)==1){
			SEX = mContext.getString(R.string.nan);
		}else {
			SEX = mContext.getString(R.string.nv);;
		}
		return SEX;
//		switch (Integer.valueOf(sex)) {
//		case 0:
//			return "未知";
//
//		case 1:
//
//			return "男";
//		case 2:
//
//			return "女";
//		case 9:
//
//			return "未说明";
//		default:
//			break;
//		}
//
//		return "";
	}

	/**
	 * 获取文字正确代码，颠倒奇数偶数字节值
	 * 
	 * @param data
	 * @return
	 */
	private byte[] getDataBytes(byte[] data) {
		byte b = 0;
		for (int i = 0; i < data.length; i++) {

			if (i % 2 == 0) {
				b = data[i];
				data[i] = data[i + 1];
			} else {
				data[i] = b;
			}
		}
		return data;
	}

	

	
	

	/**
	 * 图片解码
	 * 
	 * @param wlt
	 * @return
	 * @throws RemoteException
	 */
	private byte[] decode(byte[] wlt) throws RemoteException {
		String bmpPath = /*
						 * Environment.getExternalStorageDirectory()+"/IDCard/"+"66"
						 * +".bmp";
						 */mContext.getFileStreamPath("photo.bmp")
				.getAbsolutePath();
		String wltPath = /*
						 * Environment.getExternalStorageDirectory()+"/IDCard/"+"66"
						 * +".wlt";
						 */mContext.getFileStreamPath("photo.wlt")
				.getAbsolutePath();
		File wltFile = new File(wltPath);
		try {
			FileOutputStream fos = new FileOutputStream(wltFile);
			fos.write(wlt);
			fos.close();
		} catch (IOException e) {
			Log.e("FileIO:", e.toString());
			e.printStackTrace();
		}
		DecodeWlt dw = new com.synjones.bluetooth.DecodeWlt();
		int result = dw.Wlt2Bmp(wltPath, bmpPath);
		byte[] buffer = (byte[]) null;
		
		try {
			File bmpFile = new File(bmpPath);
			FileInputStream fin = new FileInputStream(bmpFile);
			int length = fin.available();
			buffer = new byte[length];
			fin.read(buffer);
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("IDCard",e.toString());
		}
		return buffer;
	}
	private Bitmap getBitmap2(byte[] wlt){
		WLTService dw = new WLTService();
		byte[] buffer = (byte[]) new byte[38556];
		int result = dw.wlt2Bmp(wlt, buffer);
		if (result==1) {
			Bitmap bitmap = IDPhotoHelper.Bgr2Bitmap(buffer);
			return bitmap;
		}
		return null;
	}
}

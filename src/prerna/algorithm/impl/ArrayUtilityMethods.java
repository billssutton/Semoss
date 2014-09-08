package prerna.algorithm.impl;

import java.util.Arrays;

public final class ArrayUtilityMethods {

	private static final String ERROR = "The data array either is null or does not contain any data.";
	
	private ArrayUtilityMethods() {
		
	}
	
	public static int calculateIndexOfArray(final String[] arr, final String value) {
		if(arr == null) {
			throw new NullPointerException(ERROR);
		}
		int size = arr.length;
		int index;
		for(index = 0; index < size; index++) {
			if(arr[index] != null && arr[index].equals(value)) {
				return index;
			}
		}
		return -1;
	}

	public static boolean arrayContainsValue(final Object[] arr, final Object value) {
		if(arr == null) {
			throw new NullPointerException(ERROR);
		}
		
		int size = arr.length;
		int index;
		for(index = 0; index < size; index++) {
			if(arr[index] != null && arr[index] == value) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean arrayContainsValue(final String[] arr, final String value) {
		if(arr == null) {
			throw new NullPointerException(ERROR);
		}
		int size = arr.length;
		int index;
		for(index = 0; index < size; index++) {
			if(arr[index] != null && arr[index].equals(value)) {
				return true;
			}
		}
		return false;
	}

	public static int determineLastNonNullValue(final Object[] arr) {
		int lastNonNullValue = -1;
		int size = arr.length;
		int index;
		for(index = 0; index < size; index++) {
			if(arr[index] == null) {
				continue;
			} else {
				lastNonNullValue = index;
			}
		}
		return lastNonNullValue;
	}
	
	public static Object[] trimEmptyValues(final Object[] arr) {
		int lastNonNullValue = determineLastNonNullValue(arr);
		if(lastNonNullValue == -1) {
			return null;
		}
		return Arrays.copyOfRange(arr, 0, lastNonNullValue+1);
	}

	public static boolean arrayContainsValue(final int[] arr, final int value) {
		
		if(arr == null) {
			throw new NullPointerException(ERROR);
		}
		
		int size = arr.length;
		if(size == 0) {
			return false; //empty array
		}
		
		int index;
		for(index = 0; index < size; index++) {
			if(arr[index] == value) {
				return true;
			}
		}
		return false;
	}

	public static Object[] removeAllNulls(final Object[] arr) {
		int positionInOriginalArr;
		int positionInNewArr;
		int originalLength = positionInOriginalArr = positionInNewArr = arr.length;
		while (positionInOriginalArr > 0) {
			Object value = arr[--positionInOriginalArr];
			if (value != null) {
				arr[--positionInNewArr] = value;
			}
		}
		return Arrays.copyOfRange(arr, positionInNewArr, originalLength);
	}

	public static Object[] resizeArray(final Object[] arr, final int factor) {
		return Arrays.copyOf(arr, arr.length*2); 
	}
	
	public static double[] convertObjArrToDoubleArr(final Object[] arr) {
		int size = arr.length;
		double[] retArr = new double[size];
		int index;
		for(index = 0; index < size; index++) {
			Object obj = arr[index];
			double val = 0;
			try {
				val = Double.valueOf(obj.toString());
			} catch(NumberFormatException ex) {
				throw new NumberFormatException("Value in Object array cannot be converted to double");
			}
			retArr[index] = val;
		}
		return retArr;
	}
	
	public static String[] convertObjArrToStringArr(final Object[] arr) {
		int size = arr.length;
		String[] retArr = new String[size];
		int index;
		for(index = 0; index < size; index++) {
			Object obj = arr[index];
			String val = null;
			if(obj != null) {
				val = obj.toString();
			}
			retArr[index] = val;
		}
		return retArr;
	}
	
	public static String[] getUniqueArray(final String[] arr) {
        int size = arr.length;
        String[] temp = new String[size];

		int counter = 0;
        int index;
        for (index = 0; index < size; index++) {
            if(!arrayContainsValue(temp, arr[index])) {
                temp[counter++] = arr[index];
            }
        }
        String[] uniqueArray = new String[counter];
        System.arraycopy(temp, 0, uniqueArray, 0, uniqueArray.length);
 
        return uniqueArray;
    }

}
